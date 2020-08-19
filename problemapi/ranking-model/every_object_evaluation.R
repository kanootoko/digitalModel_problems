# МОДЕЛЬ ОЦЕНКИ ОБЪЕКТОВ ГОРОДСКОЙ СРЕДЫ 
# НА ОСНОВЕ ЖАЛОБ И ОБРАЩЕНИЙ НАСЕЛЕНИЯ НА ПОРТАЛЫ ОБЩЕСТВЕННОГО УЧАСТИЯ
# РАСЧЁТ ОЦЕНКИ ДЛЯ ОТДЕЛЬНОЙ СУЩНОСТИ

filename <- 'test.csv'
object_type <- 'everything'

args = commandArgs(trailingOnly=TRUE)
if (length(args) > 0) {
    filename <- args[1]
}
if (length(args) > 1) {
    object_type <- args[2]
}

library(tidyverse)
#library(dplyr)
library(readr)

evaluate <- function(objects, group_and_mutate_func, final_denumimators, group_by_label = "Подкатегория") {
    # Расчёт оценки состояния объектов городской среды для отдельного объекта, отфильтрованного ранее
    # Включаем категории, характеризующие Зеленые зоны

    # Выделяем уникальные участки из датасета, у которых совпадает широта и долгота.
    # Считаем количество жалоб по каждому участку

    objects1 <- objects %>% add_count(Широта, Долгота)

    # Создаём переменную level. Используем переменную Подкатегория
    # для оценки критерия и  присваиваем количественные значения для Подкатегоирй - всего 5.

    objects2 <- objects1 %>%
        group_by(objects1[group_by_label], lev = as_factor(objects1[[group_by_label]])) %>%
        mutate(levels = as.numeric(lev)) %>% ungroup

    # Создаём factor переменную {S,I,C} определяющую вид Критерия для каждой подкатегории.

    # Критерии включают в себя следующие Названия:
    # Безопасность [S]
    # Физический комфорт, доступность [I]
    # Комфорт восприятия органов чувcтв [C]

    objects3 <- objects2 %>% group_and_mutate_func %>% ungroup

    # Определяем количество жалоб по каждой Подкатегории (by levels)
    # n - количество всех жалоб по данному объекту
    # nn - количество жалоб по данному объекту определенной Подкатегории

    objects4 <- objects3 %>% add_count(Широта, Долгота, n, levels, name="nn")

    # Новая переменная weight - вклад атрибута (подкатегории) в оценку (1/(1+nn))

    objects5 <- objects4 %>% group_by(levels, criteria) %>% mutate(weight = 1 / (nn + 1))

    # Cчитаем среднее по каждому критерию
    # Количество Подкатегорий внутри каждого критерия:
    # S - 1
    # I - 3
    # C - 1 

    # Безопасность
    # Для каждого двора (Широта, долгота) делим сумму весов на количество подкатегорий внутри каждого критерия. 

    safety_object <- objects5 %>% filter(criteria == "S") %>%
        group_by(Широта, Долгота, criteria) %>%
        mutate(S = sum(weight) / final_denumimators[1]) %>%
        select(Широта, Долгота, S)

    # Физический комфорт

    comf_object <- objects5 %>% filter(criteria == "I") %>%
        group_by(Широта, Долгота, criteria) %>%
        mutate(I = sum(weight) / final_denumimators[2]) %>%
        select(Широта, Долгота, I)
    
    # Эстетика

    est_object <- objects5 %>% filter(criteria == "C") %>%
        group_by(Широта, Долгота, criteria) %>%
        mutate(C = sum(weight) / final_denumimators[3]) %>%
        select(Широта, Долгота, C)

    # Cоздать датасет Широта, Долгота, S, I, C, total_index

    # Создать переменную "objectzone" с уникальными сочетаниями "широта" и "долгота" 
    # для поиска всех уникальных адресов

    objectzone <- objects %>% distinct(Широта, Долгота)

    safety_object <- ungroup(safety_object)
    comf_object <- ungroup(comf_object)
    est_object <- ungroup(est_object)

    # Объединяем в один финальный датасет адреса и  оценки по трём критериям
    objectzone1 <- objectzone %>% left_join(safety_object, by = c("Широта", "Долгота")) %>% 
        left_join(comf_object, by = c("Широта", "Долгота")) %>% 
        left_join(est_object, by = c("Широта", "Долгота"))

    # Заменяем пропущенные значения на 1 (отсутвие жалоб - высшая оценка среды)
    # Расчитываем общую оценку потребительских свойств (total_index)
    objectzone2 <- objectzone1 %>% distinct(Широта, Долгота, S, I, C) %>% 
        replace(is.na(.), 1)  %>% 
        mutate(total_index = rowMeans(select(., c("S","I","C"))))

    round(objectzone2, digits=4)
}

# Считывание данных из файла

datascore <- read_csv(filename,
    col_types = cols_only (
        ID = col_double(),
        Название = col_character(),
        Широта = col_double(),
        Долгота = col_double(),
        Подкатегория = col_character(),
        Категория = col_character()
    )
)

# Вызов функции оценки с правильными параметрами для заданного типа объектов для оценки

if (object_type == 'building') {
    result <- evaluate(
        datascore %>% filter(Категория %in% c("Дом", "Сооружение", "Квартира")),
        function(data) {
            data %>% group_by (Широта, Долгота, Подкатегория, levels) %>% 
                mutate(criteria = ifelse(levels %in% c("7", "10", "16", "18"), "S",
                    ifelse(levels %in% c("2", "1", "4", "6", "8", "9", "11", "12", "13", "17", "19"), "I",
                        ifelse(levels %in% c("3", "5", "14", "15") ,"C", "no"))))
        },
        c(4, 11, 4)
    )
} else if (object_type == 'yard') {
    result <- evaluate(
        datascore %>% filter(Категория == "Двор"),
        function(data) {
            data %>% group_by (Широта, Долгота, Название, levels) %>% 
                mutate(criteria = ifelse(levels == "4", "S",
                    ifelse(levels %in% c("2", "3", "5"), "I",
                        ifelse(levels == "1", "C", "no"))))
        },
        c(1, 3, 1)
    )
} else if (object_type == 'maf') {
    result <- evaluate(
        datascore %>% filter(Категория %in% c("Остановка общественного транспорта", "Временное сооружение")),
        function(data) {
            data %>% group_by (Широта, Долгота, Название, levels) %>% 
                mutate(criteria = ifelse(levels %in% c("3", "2"), "S",
                    ifelse(levels == "1" , "I", "no" )))
        },
        c(2, 1, 1),
        "Название"
    )
} else if (object_type == 'water') {
    result <- evaluate(
        datascore %>% filter(Категория == "Водный объект"),
        function(data) {
            data %>% group_by (Широта, Долгота, Название, levels) %>% 
                mutate(criteria = ifelse(levels == "1", "C", "no"))
        },
        c(1, 1, 1),
        "Название"
    )
} else if (object_type == 'greenzone') {
    result <- evaluate(
        datascore %>% filter(Категория %in% c("Парк, сад, бульвар, сквер", "Кладбище")),
        function(data) {
            data %>%group_by (Широта, Долгота, Название, levels) %>% 
                mutate(criteria = ifelse(levels %in% c("1", "14"), "S",
                    ifelse(levels %in% c("3", "7", "8", "11", "12", "15"), "I",
                        ifelse(levels %in% c("2", "4", "5", "6", "9", "10", "13"), "C", "no"))))
        },
        c(2, 6, 7),
        "Название"
    )
} else if (object_type == 'uds') {
    result <- evaluate(
        datascore
            %>% filter(Категория %in% c("Мост", "Улица", "Общественный транспорт", "Территория Санкт-Петербурга"))
            %>% filter(Подкатегория %in% c("Благоустройство", "Повреждения или неисправность элементов уличной инфраструктуры",
                "Техническое и санитарное состояние транспортных средств")),
        function(data) {
            data %>% group_by (Широта, Долгота, Подкатегория, levels) %>% 
                mutate(criteria = ifelse(levels == "3", "S",
                    ifelse(levels == "2", "I",
                        ifelse(levels == "1", "C", "no"))))
        },
        c(1, 1, 1)
    )
} else {
    print('Error, unknown object type')
    quit(1)
}

write_csv(round(result, digits=4), paste(substr(filename, 1, nchar(filename) - 4), '_output', '.csv', sep=''))


