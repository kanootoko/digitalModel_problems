import time
from typing import Callable, Optional

try:
    import psycopg2
    import argparse
    import pandas
except ModuleNotFoundError:
    print('Some of the modules not found. Try executing "python -m pip install psycopg2 argparse pandas"')
    exit(1)

_VERBOSE_NUMBER = 50000

def _prepare_string(s: str):
    return s.replace("'", "''")

def create_tables(conn_or_file):
    creation_script = '''
        CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;

        CREATE TABLE IF NOT EXISTS Problems (
            id serial primary key not null,
            outerID int not null,
            name varchar not null,
            district varchar not null,
            status varchar not null,
            creationDate timestamp not null,
            updateDate timestamp not null,
            description varchar not null,
            userName varchar not null,
            userID int not null,
            coordinates geometry not null,
            address varchar,
            municipality varchar not null,
            reason varchar not null,
            category varchar not null,
            subcategory varchar not null
        );
        
        CREATE TABLE evaluation_municipalities (
            municipality_name varchar primary key not null,
            date varchar(7),
            s float,
            i float,
            c float,
            total_value float,
            objects_number int not null
        );
        
        CREATE TABLE evaluation_districts (
            district_name varchar primary key not null,
            date varchar(7),
            s float,
            i float,
            c float,
            total_value float,
            objects_number int not null
        );
        '''
    if isinstance(conn, psycopg2.extensions.connection):
        cur = conn.cursor()
        cur.execute(creation_script)
        conn.commit()
    else:
        print(creation_script, file=conn_or_file)


def insert_to_db(conn: Optional[psycopg2.extensions.connection], path_to_csv: str, verbose: bool, file, min_outer_id: Optional[int], insert_size: Optional[int]):
    log: Callable[[str], None]
    if verbose:
        log = lambda s: print(s)
    else:
        log = lambda s: None
    insertion_string = \
            'INSERT INTO Problems (OuterID, Name, district, Status, CreationDate, UpdateDate, Description,' \
            ' UserName, UserID, Coordinates, Address, Municipality, Reason, Category, Subcategory) VALUES'

    cur: Optional[psycopg2.extensions.cursor] = None
    if conn is not None:
        cur = conn.cursor()
    data = pandas.read_csv(path_to_csv)
    log('Data is read')

    assert cur is None and min_outer_id is not None and file is not None or cur is not None and min_outer_id is None, \
        'Cursor must be valid when no min_OuterID is provided'

    if min_outer_id is None and cur is not None:
        cur.execute('select max(OuterID) from problems')
        min_outer_id = 0
        res = cur.fetchall()
        if res[0][0] is not None:
            min_outer_id = int(res[0][0])
        log(f'Starting from minimum OuterID: {min_outer_id}')
    if file is None:
        cur.execute(
            'prepare problem_insertion as ' + insertion_string +
            ' ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)'
        )
        log('Prepared statement')

    start_time = time.time()
    loaded = 0
    try:
        for i, line in data.iterrows():
            if line['Внешний ID'] <= min_outer_id:
                continue
            if line['Статус'] not in ('Завершено: Пользователь удовлетворен решением проблемы', 'Завершено: Автоматически',
                                      'Рассмотрение', 'Промежуточный ответ', 'Получен ответ', 'Принят',
                                      'Завершено: Народный контролер подтвердил решение проблемы', 'Модерация'
                                      ) \
                or line['Муниципальное образование'].endswith('(искл.)') \
                or line['Название'].endswith('(искл.)') \
                or line['Причина обращения'].endswith('(искл.)'):
                    continue
            loaded += 1
            if (loaded % _VERBOSE_NUMBER == 0):
                log(f'{loaded:7} lines inserted, {i:7} processed totally, {len(data):7} total')
            if file is None:
                cur.execute(
                    'execute problem_insertion (%s, %s, %s, %s, to_timestamp(%s), to_timestamp(%s), %s, %s, %s, ST_SetSRID(ST_Point(%s, %s), 4326), %s, %s, %s, %s, %s)',
                    (
                        line['Внешний ID'], line['Название'], line['Район'], line['Статус'],
                        time.mktime(time.strptime(line['Дата создания'], '%d.%m.%Y %H:%M:%S')),
                        time.mktime(time.strptime(line['Дата последнего обновления'], '%d.%m.%Y %H:%M:%S')),
                        line['Описание'] if isinstance(line['Описание'], str) and line['Описание'] != 'NaN' else line['Причина обращения'],
                        line['Пользователь'], line['ID Пользователя'][10:-1], line['Долгота'], line['Широта'],
                        None if type(line['Адрес']) is not str else line['Адрес'][19:] if line['Адрес'].startswith('г.Санкт-Петербург, ') else line['Адрес'],
                        line['Муниципальное образование'], line['Причина обращения'],
                        line['Категория'], line['Подкатегория']
                    )
                )
            else:
                if insert_size is not None and (loaded - 1) % insert_size != 0 or insert_size is None and loaded > 1:
                    print(', ', file=file)
                else:
                    print(';', file=file)
                    print(insertion_string, file=file)
                print(
                    "({}, '{}', '{}', '{}', to_timestamp({}), to_timestamp({}), '{}', '{}', {}, ST_SetSRID(ST_Point({}, {}), 4326), {}, '{}', '{}', '{}', '{}')".format(
                        line['Внешний ID'], _prepare_string(line['Название']),
                        _prepare_string(line['Район']), _prepare_string(line['Статус']),
                        time.mktime(time.strptime(line['Дата создания'], '%d.%m.%Y %H:%M:%S')),
                        time.mktime(time.strptime(line['Дата последнего обновления'], '%d.%m.%Y %H:%M:%S')),
                        _prepare_string(line['Описание']) if isinstance(line['Описание'], str) and line['Описание'] != 'NaN' else _prepare_string(line['Причина обращения']),
                        _prepare_string(line['Пользователь']) if isinstance(line['Пользователь'], str) else 'null',
                        line['ID Пользователя'][10:-1], line['Долгота'], line['Широта'],
                        'null' if type(line['Адрес']) is not str else f"'{_prepare_string(line['Адрес'][19:])}'" if line['Адрес'].startswith('г.Санкт-Петербург, ') else f"'{_prepare_string(line['Адрес'])}'",
                        _prepare_string(line['Муниципальное образование']), _prepare_string(line['Причина обращения']),
                        _prepare_string(line['Категория']), _prepare_string(line['Подкатегория'])
                    ), file=file, end='')
    except:
        if conn is not None:
            cur.close()
            conn.rollback()
        print('Rolling back, exception occured:')
        raise
    else:
        if conn is not None:
            cur.close()
            conn.commit()
        print(f'{loaded} objects are loaded')
    finally:
        if file is None:
            cur = conn.cursor()
            cur.execute('deallocate problem_insertion')
            log('deallocated prepared statement')
            cur.close()
            conn.commit()
        else:
            file.close()
        print(f'Execution took {time.time() - start_time:2f} seconds')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Creates problems table and inserts data to it')
    parser.add_argument('-H', '--host', action='store', dest='host',
                        help='postgres host address [default: localhost]', type=str, default='localhost')
    parser.add_argument('-p', '--port', action='store', dest='port',
                        help='postgres port number [default: 5432]', type=int, default=5432)
    parser.add_argument('-d', '--database', action='store', dest='database',
                        help='postgres database name [default: problems]', type=str, default='problems')
    parser.add_argument('-u', '--user', action='store', dest='user',
                        help='postgres user name [default: postgres]', type=str, default='postgres')
    parser.add_argument('-P', '--password', action='store', dest='password',
                        help='postgres user password [default: postgres]', type=str, default='postgres')
    parser.add_argument('-c', '--csv_path', action='store', dest='path_to_csv',
                        help='full path to csv file with problems [default: problems_export_2020-05-27.csv]', type=str, default='problems_export_2020-05-27.csv')
    parser.add_argument('-g', '--generate', action='store', dest='generate',
                        help='name of sql script to generate without inserting anything in the base', type=str, default=None)
    parser.add_argument('-m', '--min_outer_id', action='store', dest='min_outer_id',
                        help='minimal OuterID to insert, only used with -g, cuts of connection to DB', type=int, default=None)
    parser.add_argument('-s', '--insert_size', action='store', dest='insert_size',
                        help='number of values per one insert, only used with -g', type=int, default=None)
    parser.add_argument('-v', '--verbose', action='store_true', dest='verbose',
                        help=f'output progress (every {_VERBOSE_NUMBER} entities)')
    args = parser.parse_args()

    assert args.min_outer_id is None or args.min_outer_id is not None and args.generate is not None, '-m option is only used with -g'
    assert args.insert_size is None or args.insert_size is not None and args.generate is not None, '-s option is only used with -g'

    conn: Optional[psycopg2.extensions.connection] = None
    curr: Optional[psycopg2.extensions.cursor] = None
    sql_file = None
    if args.generate is not None:
        if args.verbose:
            print(f'Generating sql script "{args.generate}"')
        sql_file = open(args.generate, 'w', encoding='utf-8')
    if args.min_outer_id is None:
        print('Connecting to the database')
        conn = psycopg2.connect(
            f'host={args.host} port={args.port} dbname={args.database} user={args.user} password={args.password}')
        cur = conn.cursor()
        create_tables(conn)
        insert_to_db(conn, args.path_to_csv, args.verbose, sql_file, None, args.insert_size)
        conn.close()
    else:
        if args.verbose:
            print(f'Inserting from the min_OuterID = {args.min_outer_id}')
        create_tables(sql_file)
        insert_to_db(None, args.path_to_csv, args.verbose, sql_file, args.min_outer_id, args.insert_size)
