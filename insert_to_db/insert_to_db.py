import time
try:
    import psycopg2
    import argparse
    import pandas
except ModuleNotFoundError:
    print('Some of the modules not found. Try "python -m pip install psycopg2 argparse pandas"')
    exit(1)

def create_tables(cur):
    cur.execute('''
        create extension if not exists postgis with schema public;

        create table if not exists Problems (
            ID serial primary key not null,
            OuterID int not null,
            Name varchar not null,
            Region varchar not null,
            Status varchar not null,
            CreationDate timestamp not null,
            UpdateDate timestamp not null,
            Description varchar not null,
            UserName varchar not null,
            UserID int not null,
            Coordinates geometry not null,
            Address varchar,
            Municipality varchar not null,
            Reason varchar not null,
            Category varchar not null,
            Subcategory varchar not null
        )''')

    conn.commit()

def insert_to_db(cur, path_to_csv: str):
    data = pandas.read_csv(path_to_csv)

    cur.execute('select max(OuterID) from problems')
    min_id = 0
    res = cur.fetchall()
    if res[0][0] is not None:
        min_id = res[0][0]
    cur.execute(
        'prepare problem_insertion as'
        ' insert into Problems (OuterID, Name, Region, Status, CreationDate, UpdateDate, Description,'
        ' UserName, UserID, Coordinates, Address, Municipality, Reason, Category, Subcategory) values'
        ' ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)'
    )
    start_time = time.time()
    loaded = 0
    try:
        for _, line in data.iterrows():
            if line['Внешний ID'] <= min_id:
                continue
            if line['Статус'] not in ('Завершено: Пользователь удовлетворен решением проблемы', 'Завершено: Автоматически',
                            'Рассмотрение', 'Промежуточный ответ', 'Получен ответ', 'Принят',
                            'Завершено: Народный контролер подтвердил решение проблемы', 'Модерация'
            ):
                continue
            loaded += 1
            cur.execute(
                'execute problem_insertion (%s, %s, %s, %s, to_timestamp(%s), to_timestamp(%s), %s, %s, %s, ST_SetSRID(ST_Point(%s, %s), 4326), %s, %s, %s, %s, %s)',
                (
                    line['Внешний ID'], line['Название'], line['Район'], line['Статус'],
                    time.mktime(time.strptime(line['Дата создания'], '%d.%m.%Y %H:%M:%S')),
                    time.mktime(time.strptime(line['Дата последнего обновления'], '%d.%m.%Y %H:%M:%S')),
                    line['Описание'], line['Пользователь'], line['ID Пользователя'][10:-1],
                    line['Долгота'], line['Широта'], None if type(line['Адрес']) is not str else line['Адрес'][19:] if line['Адрес'].startswith('г.Санкт-Петербург, ') else line['Адрес'],
                    line['Муниципальное образование'], line['Причина обращения'],
                    line['Категория'], line['Подкатегория']
                )
            )
    except:
        cur.close()
        conn.rollback()
        print('Rolling back, exception occured:')
        raise
    else:
        cur.close()
        conn.commit()
        print(f'{loaded} objects are loaded')
    finally:
        cur = conn.cursor()
        cur.execute('deallocate problem_insertion')
        cur.close()
        conn.commit()
        print(f'Insertion took {time.time() - start_time:2f} seconds')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Creates problems table and inserts data to it')
    parser.add_argument('-H', '--host', action='store', dest='host', help='postgres host address [default: localhost]', type=str, default='localhost')
    parser.add_argument('-p', '--port', action='store', dest='port', help='postgres port number [default: 5432]', type=int, default=5432)
    parser.add_argument('-d', '--database', action='store', dest='database', help='postgres database name [default: problems]', type=str, default='problems')
    parser.add_argument('-u', '--user', action='store', dest='user', help='postgres user name [default: postgres]', type=str, default='postgres')
    parser.add_argument('-P', '--password', action='store', dest='password', help='postgres user password [default: postgres]', type=str, default='postgres')
    parser.add_argument('-c', '--csv_path', action='store', dest='path_to_csv', help='full path to csv file with problems [default: problems_export_2020-05-27.csv]', type=str, default='problems_export_2020-05-27.csv')
    args = parser.parse_args()

    print('Connecting to the database')
    conn = psycopg2.connect(f'host={args.host} port={args.port} dbname={args.database} user={args.user} password={args.password}')
    cur = conn.cursor()
    create_tables(cur)
    insert_to_db(cur, args.path_to_csv)
    cur.close()
