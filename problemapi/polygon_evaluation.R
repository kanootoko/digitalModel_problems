# ОЦЕНКА ТЕРРИТОРИЙ ПО НЕСКОЛЬКИМ ВИДАМ СУЩНОСТЕЙ
# Расчёт для выделенного полигона

filename <- 'test.csv'

args = commandArgs(trailingOnly=TRUE)
if (length(args) > 0) {
        filename <- args[1]
}


# Загружаем библиотеки
library(tidyverse)
library(readr)

# Загружаем датасет с жалобами из файла "problems_export_2020-05-27.csv" с необходимыми переменными
datascore1 <- read_csv(filename,col_types = cols_only(
        ID = col_double(),
        Название = col_character(),
        Широта = col_double(),
        Долгота = col_double(),
        Подкатегория = col_character(),
        Категория = col_character()))

# ЗДЕСЬ НА ВХОД ПОДАЮТСЯ ЖАЛОБЫ С ВЫБРАННОГО ПОЛИГОНА

# На примере случайной выборки из datascore (1000 наблюдений) рассматриваем расчёт индекса оценки
# территорий на выбранном полигоне.
#datascore1 <- datascore1 %>% sample_n(size = 1000)

###### 1. ЗДАНИЯ
poly <- datascore1 %>% filter(Категория == "Дом"| Категория == "Сооружение"|Категория == "Квартира")
poly1 <- poly %>% add_count(Широта, Долгота)

# Создаём переменную level: присваиваем количественные значения для Подкатегорий - всего 19.

poly2 <- poly1 %>% group_by(Подкатегория) %>%
        mutate(lev = as_factor(Подкатегория)) %>% 
        mutate(levels = as.numeric(lev))
poly2 <- ungroup(poly2)

#Создаём factor переменную {S,I, C} определяющую вид Критерия для каждой подкатегории.

# Критерии включают в себя следующие Подкатегории:
# Безопасность [S] - levels {16,18,10,7}
# Физический комфорт, доступность [I] - levels {2, 1, 4, 6, 8, 9,11,12,13,17,19}
# Комфорт восприятия органов чувcтв [C] - levels {3, 5, 14, 15}

poly3 <- poly2 %>% group_by (Широта, Долгота, Подкатегория, levels) %>% 
        mutate(criteria = ifelse(levels == "7"| levels == "10"| levels == "16"| levels == "18","S",
                                 ifelse(levels == "2"| levels == "1"|levels == "4"| levels == "6"|
                                                levels == "8"| levels == "9"| levels == "11"|
                                                levels == "12"|levels == "13"|levels == "17"|levels == "19", "I",
                                        ifelse(levels == "3"|levels == "5"|levels == "14"|levels == "15","C", "no"))))

#poly3 %>% group_by (Широта, Долгота,criteria) %>% select(Широта, Долгота, n, Подкатегория, levels,criteria)
poly3 <- ungroup (poly3)

# Определяем количество жалоб по каждой Подкатегории (by levels)
# n - количество всех жалоб по данному дому
# nn - количество жалоб по данному дому определенной Подкатегории

poly4 <- poly3 %>% add_count(Широта, Долгота,n,criteria) 
#poly4 %>% select(Широта, Долгота,levels, n, nn,criteria)

# Проверка 
#poly4  %>% arrange(Широта) %>% filter(criteria =="C") %>% select(Широта, Долгота,levels, n, nn,criteria)

# Новая переменная weight - вклад атрибута (подкатегории) в оценку (1/(1+nn))

poly5 <- poly4 %>% group_by(levels,criteria) %>%
        mutate(weight = 1 / (nn + 1))
#poly5 %>% select(Подкатегория, n, nn, weight)

# Cчитаем среднее по каждому критерию
# Количество Подкатегорий внутри каждого критерия:
# S - 4
# I - 11
# C - 4 

# Безопасность
# Для каждого дома (Широта, долгота) делим сумму весов на 4. 

safety_poly <- poly5 %>% filter(criteria == "S") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(S = sum(weight) / 4) %>% 
        select(Широта, Долгота, S)

# Физический комфорт

comf_poly <- poly5 %>% filter(criteria == "I") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(I = sum(weight) / 11) %>% 
        select(Широта, Долгота, I)

# Эстетика

est_poly <- poly5 %>% filter(criteria == "C") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(C = sum(weight) / 4) %>% 
        select(Широта, Долгота, C)

# Вывод результата. 
# Создать переменную "build" с уникальными сочетаниями "широта" и "долгота" 
# для поиска всех уникальных адресов

build <- poly %>% distinct(Широта, Долгота)

safety_poly <- ungroup(safety_poly)
comf_poly <- ungroup(comf_poly)
est_poly <- ungroup(est_poly)

# Объединяем в один финальный датасет адреса и  оценки по трём критериям

build1 <- build %>% left_join(safety_poly, by = c("Широта", "Долгота")) %>% 
        left_join(comf_poly, by = c("Широта", "Долгота")) %>%
        left_join(est_poly, by = c("Широта", "Долгота"))

# Заменяем пропущенные значения на 1 (отсутвие жалоб - высшая оценка качества среды)
# Расчитываем общую оценку потребительских свойств (total_index)
build2 <- build1 %>% distinct(Широта, Долгота, S, I, C) %>% 
        replace(is.na(.), 1) %>% 
        mutate(total_index = rowMeans(select(.,c("S","I","C"))))
# build2

# Оценка для всего полигона. 
# Для общей оценки состояния зданий на территории нужно взять среднее арифметическое всех оценок по критериям для зданий. 
# Расчитываем среднюю оценку по каждому критерию для всех зданий на полигоне. 

poly_result <- build2 %>% 
        summarise_at(c("S", "I", "C", "total_index"), mean)
# poly_result


##### 2.УДС
# Формируем датасет с категориями, характеризующими УДС
poly_uds <- datascore1 %>% filter(Категория == "Мост" | Категория == "Улица" |
                                             Категория == "Остановка общественного транспорта"|
                                             Категория == "Общественный транспорт"|
                                             Категория == "Территория Санкт-Петербурга") %>% 
        filter(Подкатегория == "Благоустройство"|
                                   Подкатегория == "Повреждения или неисправность элементов уличной инфраструктуры"|
                                   Подкатегория == "Техническое и санитарное состояние транспортных средств")
poly_uds1 <- poly_uds %>% add_count(Широта, Долгота)

# Создаём переменную level: присваиваем количественные значения для Подкатегорий - всего 3.

poly_uds2 <- poly_uds1 %>% group_by(Подкатегория) %>%
        mutate(lev = as_factor(Подкатегория)) %>% 
        mutate(levels = as.numeric(lev))

# poly_uds2 %>% select(ID, Широта, Долгота, n, Подкатегория, levels)
poly_uds2 <- ungroup(poly_uds2)

#Создаём factor переменную {S,I, C} определяющую вид Критерия для каждой подкатегории.

# Критерии включают в себя следующие Подкатегории:
# Безопасность [S] - 3
# Физический комфорт, доступность [I] - 2
# Комфорт восприятия органов чувcтв [C] - 1

poly_uds3 <- poly_uds2 %>% group_by (Широта, Долгота, Подкатегория, levels) %>% 
        mutate(criteria = ifelse(levels == "3","S",
                                 ifelse(levels == "2", "I",
                                        ifelse(levels == "1","C", "no"))))

#poly_uds3 %>% group_by (Широта, Долгота,criteria) %>% select(Широта, Долгота, n, Подкатегория, levels,criteria) 
poly_uds3 <- ungroup (poly_uds3)
# Определяем количество жалоб по каждой Подкатегории (by levels)
# n - количество всех жалоб по данному дому
# nn - количество жалоб по данному дому определенной Подкатегории

poly_uds4 <- poly_uds3 %>% add_count(Широта, Долгота,n,criteria)
#poly_uds4%>% select(Широта, Долгота,levels, n, nn,criteria)

# Проверка 
#poly_uds4  %>% arrange(Широта) %>% filter(levels ==2) %>% select(Широта, Долгота,levels, n, nn,criteria)


# Новая переменная weight - вклад атрибута (подкатегории) в оценку (1/(1+nn))

poly_uds5 <- poly_uds4 %>% group_by(levels,criteria) %>%
        mutate(weight = 1 / (nn + 1))
#poly5 %>% select(Подкатегория, n, nn, weight)

# Cчитаем среднее по каждому критерию
# Количество Подкатегорий внутри каждого критерия:
# S - 1
# I - 1
# C - 1 

# Безопасность
# Для каждого дома (Широта, долгота) делим сумму весов на 4. 

safety_uds_poly <- poly_uds5 %>% filter(criteria == "S") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(S = sum(weight) / 1) %>% 
        select(Широта, Долгота, S)

# Физический комфорт

comf_uds_poly <- poly_uds5 %>% filter(criteria == "I") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(I = sum(weight) / 1) %>% 
        select(Широта, Долгота, I)

# Эстетика

est_uds_poly <- poly_uds5 %>% filter(criteria == "C") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(C = sum(weight) / 1) %>% 
        select(Широта, Долгота, C)

# Вывод результата. 
# Создать переменную "road_poly" с уникальными сочетаниями "широта" и "долгота" 
# для поиска всех уникальных адресов

road_poly <- poly_uds %>% distinct(Широта, Долгота)

safety_uds_poly <- ungroup(safety_uds_poly)
comf_uds_poly <- ungroup(comf_uds_poly)
est_uds_poly <- ungroup(est_uds_poly)

# Объединяем в один финальный датасет адреса и  оценки по трём критериям

road_poly1 <- road_poly %>% left_join(safety_uds_poly, by = c("Широта", "Долгота")) %>% 
        left_join(comf_uds_poly, by = c("Широта", "Долгота")) %>%
        left_join(est_uds_poly, by = c("Широта", "Долгота"))

# Заменяем пропущенные значения на 1 (отсутвие жалоб - высшая оценка среды)
# Расчитываем общую оценку потребительских свойств (total_index)
road_poly2 <- road_poly1 %>% distinct(Широта, Долгота, S, I, C) %>% 
        replace(is.na(.), 1) %>% 
        mutate(total_index = rowMeans(select(.,c("S","I","C"))))
# road_poly2

# Оценка качества УДС для всего полигона.
# Расчитываем среднюю оценку по каждому критерию

poly_uds_result <- road_poly2 %>% 
        summarise_at(c("S", "I", "C", "total_index"), mean)
# poly_uds_result        


##### 3. ЗЕЛЕНЫЕ ЗОНЫ
# Включаем категории, характеризующие Зеленые зоны
green <- datascore1 %>% filter(Категория == "Парк, сад, бульвар, сквер" | Категория == "Кладбище")

# Выделяем уникальные участки из датасета, у которых совпадает широта и долгота.
# Считаем количество жалоб по каждому участку

green1 <- green %>% add_count(Широта, Долгота)
#green1 %>% select(Широта, Долгота,n)

# Создаём переменную level. Так как подкатегория всего одна, используем переменную
# 'Название' для оценки критерия и  присваиваем количественные значения для Названий - всего 15.

green2 <- green1 %>% group_by(Название) %>%
        mutate(lev = as_factor(Название)) %>% 
        mutate(levels = as.numeric(lev))

# green2 %>% select(n, Название, levels)
green2 <- ungroup(green2)

#Создаём factor переменную {S,I, C} определяющую вид Критерия для каждой подкатегории.

# Критерии включают в себя следующие Названия:
# Безопасность [S] - {1,14}
# Физический комфорт, доступность [I] - {3,7,8,11,12,15}
# Комфорт восприятия органов чувcтв [C] - {2,4,5,6,9,10,13}

green3 <- green2 %>% group_by (Широта, Долгота, Название, levels) %>% 
        mutate(criteria = ifelse(levels == "1"| levels == "14","S",
                                 ifelse(levels == "3"|levels == "7"| levels == "8"| levels == "11"| levels == "12"| levels == "15", "I",
                                        ifelse(levels == "2"|levels == "4"| levels == "5"| levels == "6"| levels == "9"|
                                                       levels == "10"| levels == "13","C", "no"))))

#green3 %>% group_by (Широта, Долгота,criteria) %>% select(Широта, Долгота, n, Название, levels,criteria) 
green3 <- ungroup (green3)

# Определяем количество жалоб по каждой Подкатегории (by levels)
# n - количество всех жалоб по данному объекту
# nn - количество жалоб по данному объекту определенной Подкатегории

green4 <- green3 %>% add_count(Широта, Долгота,n,criteria)
#green4%>% select(Широта, Долгота,levels, n, nn,criteria)

# Проверка 
#green4  %>% arrange(Широта) %>% filter(levels ==3) %>% select(Широта, Долгота,levels, n, nn,criteria)

# Новая переменная weight - вклад атрибута (подкатегории) в оценку (1/(1+nn))

green5 <- green4 %>% group_by(levels,criteria) %>%
        mutate(weight = 1 / (nn + 1))
# green5 %>% select(Подкатегория, n, nn, weight)

# Cчитаем среднее по каждому критерию
# Количество Подкатегорий внутри каждого критерия:
# S - 2
# I - 6
# C - 7 

# Безопасность
# Для каждого дома (Широта, долгота) делим сумму весов на количество подкатегорий внутри каждого критерия. 

safety_green <- green5 %>% filter(criteria == "S") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(S = sum(weight) / 2) %>% 
        select(Широта, Долгота, S)

# Физический комфорт

comf_green <- green5 %>% filter(criteria == "I") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(I = sum(weight) / 6) %>% 
        select(Широта, Долгота, I)

# Эстетика

est_green <- green5 %>% filter(criteria == "C") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(C = sum(weight) / 7) %>% 
        select(Широта, Долгота, C)

# Cоздать датасет Широта, Долгота, S, I, C, total_index

# Создать переменную "greenzone" с уникальными сочетаниями "широта" и "долгота" 
# для поиска всех уникальных адресов

greenzone <- green %>% distinct(Широта, Долгота)

safety_green <- ungroup(safety_green)
comf_green <- ungroup(comf_green)
est_green <- ungroup(est_green)

# Объединяем в один финальный датасет адреса и  оценки по трём критериям
greenzone1 <- greenzone %>% left_join(safety_green, by = c("Широта", "Долгота")) %>% 
        left_join(comf_green, by = c("Широта", "Долгота")) %>%
        left_join(est_green, by = c("Широта", "Долгота"))
# greenzone1 %>% group_by(Широта, Долгота) %>% select(S,I,C)

# Заменяем пропущенные значения на 1 (отсутвие жалоб - высшая оценка среды)
# Расчитываем общую оценку потребительских свойств (total_index)
greenzone2 <- greenzone1 %>% distinct(Широта, Долгота, S, I, C) %>% 
        replace(is.na(.), 1) %>% 
        mutate(total_index = rowMeans(select(.,c("S","I","C"))))
# greenzone2

# Оценка зеленых зон для всего полигона.
# Расчитываем среднюю оценку по каждому критерию

greenzone_result <- greenzone2 %>% 
        summarise_at(c("S", "I", "C", "total_index"), mean)
# greenzone_result

# 4. ВОДНЫЕ ОБЪЕКТЫ
# Включаем категории, характеризующие Водные объекты

# Включаем категории, характеризующие Водные объекты
water <- datascore1 %>% filter(Категория == "Водный объект")

# Выделяем уникальные участки из датасета, у которых совпадает широта и долгота.
# Считаем количество жалоб по каждому участку

water1 <- water %>% add_count(Широта, Долгота)
# water1 %>% select(Широта, Долгота,n)

# Создаём переменную level. Так как подкатегория и название всего одно - "Мусор в воде или на берегу водного объекта"
# мождно оценить только один критерий Комфорт восприятия органов чувcтв [C], и на основе его вывести общий критерий

water2 <- water1 %>% group_by(Название) %>%
        mutate(lev = as_factor(Название)) %>% 
        mutate(levels = as.numeric(lev))

# water2 %>% select(n, Название, levels)
water2 <- ungroup(water2)

#Создаём factor переменную {S,I, C} определяющую вид Критерия для каждой подкатегории.

# Критерии включают в себя следующие Названия:
# Комфорт восприятия органов чувcтв [C] - 1

water3 <- water2 %>% group_by (Широта, Долгота, Название, levels) %>% 
        mutate(criteria = ifelse(levels == "1","С","no"))
water3 <- ungroup (water3)

# Определяем количество жалоб по каждой Подкатегории (by levels)
# n - количество всех жалоб по данному объекту
# nn - количество жалоб по данному объекту определенной Подкатегории

water4 <- water3 %>% add_count(Широта, Долгота,n,criteria)

# Новая переменная weight - вклад атрибута (подкатегории) в оценку (1/(1+nn))

water5 <- water4 %>% group_by(levels,criteria) %>%
        mutate(weight = 1 / (nn + 1))
# water5 %>% select(Подкатегория, n, nn, weight)

# Cчитаем среднее по каждому критерию
# Количество Подкатегорий внутри каждого критерия:
# C - 1 

# Эстетика

est_water <- water5 %>%  
        group_by(Широта, Долгота, criteria) %>% 
        mutate(C = sum(weight) / 1) %>% 
        select(Широта, Долгота, C)


# Создать переменную "waterzone" с уникальными сочетаниями "широта" и "долгота" 
# для поиска всех уникальных адресов

waterzone <- water %>% distinct(Широта, Долгота)
est_water <- ungroup(est_water)

# Объединяем в один финальный датасет адреса и  оценки по трём критериям
waterzone1 <- waterzone %>% left_join(est_water, by = c("Широта", "Долгота"))
# waterzone1 %>% group_by(Широта, Долгота) %>% select(C)

# Заменяем пропущенные значения на 1 (отсутвие жалоб - высшая оценка среды)
# Так как жалоб по безопасности "S" и комфорту "I" не поступает, эта оценка принимается за 1 (максимальная положительная)
# Расчитываем общую оценку потребительских свойств (total_index)
waterzone2 <- waterzone1 %>% distinct(Широта, Долгота, C) %>% 
        mutate (S = 1) %>% mutate(I = 1) %>% 
        replace(is.na(.), 1) %>% 
        mutate(total_index = rowMeans(select(.,c("S","I","C")))) %>% select(Широта, Долгота, S,I,C, total_index)
# waterzone2

# Общая оценка всех водных объектов на территории  полигона.
# Расчитываем среднюю оценку по каждому критерию

waterzone_result <- waterzone2 %>% 
        summarise_at(c("S", "I", "C", "total_index"), mean)
# waterzone_result

###### 5. MAФ

# Включаем категории, характеризующие Зеленые зоны
maf <- datascore1 %>% filter(Категория == "Остановка общественного транспорта" | Категория == "Временное сооружение")

# Выделяем уникальные участки из датасета, у которых совпадает широта и долгота.
# Считаем количество жалоб по каждому участку

maf1 <- maf %>% add_count(Широта, Долгота)
#maf1 %>% select(Широта, Долгота,n)

# Создаём переменную level. Так как подкатегория всего одна, используем переменную
# `Название` для оценки критерия и  присваиваем количественные значения для Названий - всего 3.

maf2 <- maf1 %>% group_by(Название) %>%
        mutate(lev = as_factor(Название)) %>% 
        mutate(levels = as.numeric(lev))

#maf2 %>% select(n, Название, levels)
maf2 <- ungroup(maf2)

#Создаём factor переменную {S,I, C} определяющую вид Критерия для каждой подкатегории
# Нету вида жалоб, подходящего для оценки эстетики. Поэтому Критерий эстетики для МАФ не рассчитывается

# Критерии включают в себя следующие Названия:
# Безопасность [S] - {1,2}
# Физический комфорт, доступность [I] - {3}
# Комфорт восприятия органов чувcтв [C] - нет

maf3 <- maf2 %>% group_by (Широта, Долгота, Название, levels) %>% 
        mutate(criteria = ifelse(levels == "3"|levels == "2","S",
                                 ifelse(levels == "1" , "I", "no" )))

#maf3 %>% group_by (Широта, Долгота,criteria) %>% select(Широта, Долгота, n, Название, levels,criteria) 
maf3 <- ungroup (maf3)

# Определяем количество жалоб по каждой Подкатегории (by levels)
# n - количество всех жалоб по данному объекту
# nn - количество жалоб по данному объекту определенной Подкатегории

maf4 <- maf3 %>% add_count(Широта, Долгота,n,criteria)
#maf4%>% select(Широта, Долгота,levels, n, nn,criteria)

# Проверка 
#maf4  %>% arrange(Широта) %>% filter(levels ==3) %>% select(Широта, Долгота,levels, n, nn,criteria)

# Новая переменная weight - вклад атрибута (подкатегории) в оценку (1/(1+nn))

maf5 <- maf4 %>% group_by(levels,criteria) %>%
        mutate(weight = 1 / (nn + 1))
# maf5 %>% select(Подкатегория, n, nn, weight)

# Cчитаем среднее по каждому критерию
# Количество Подкатегорий внутри каждого критерия:
# S - 2
# I - 1
# C - 0 

# Безопасность
# Для каждого дома (Широта, долгота) делим сумму весов на количество подкатегорий внутри каждого критерия. 

safety_maf <- maf5 %>% filter(criteria == "S") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(S = sum(weight) / 2) %>% 
        select(Широта, Долгота, S)

# Физический комфорт

comf_maf <- maf5 %>% filter(criteria == "I") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(I = sum(weight) / 1) %>% 
        select(Широта, Долгота, I)


# Создать переменную "mafzone" с уникальными сочетаниями "широта" и "долгота" 
# для поиска всех уникальных адресов

mafzone <- maf %>% distinct(Широта, Долгота)

safety_maf <- ungroup(safety_maf)
comf_maf <- ungroup(comf_maf)

# Объединяем в один финальный датасет адреса и  оценки по трём критериям
mafzone1 <- mafzone %>% left_join(safety_maf, by = c("Широта", "Долгота")) %>% 
        left_join(comf_maf, by = c("Широта", "Долгота")) 

# mafzone1 %>% group_by(Широта, Долгота) %>% select(S,I)

# Заменяем пропущенные значения на 1 (отсутвие жалоб - высшая оценка среды)
# Так как жалоб по критерию эстетики "C" нет, то оценка для всех объектов определяется как 1
# Расчитываем общую оценку потребительских свойств (total_index)
mafzone2 <- mafzone1 %>% distinct(Широта, Долгота, S, I) %>% 
        replace(is.na(.), 1) %>% mutate(C = 1) %>% 
        mutate(total_index = rowMeans(select(.,c("S","I","C"))))
# mafzone2

# Оценка для всего полигона.
# Расчитываем среднюю оценку по каждому критерию

mafzone_result <- mafzone2 %>% 
        summarise_at(c("S", "I", "C", "total_index"), mean)
# mafzone_result

# 6. ДВОР

# Включаем категории, характеризующие Зеленые зоны
dvor <- datascore1 %>% filter(Категория == "Двор")

# Выделяем уникальные участки из датасета, у которых совпадает широта и долгота.
# Считаем количество жалоб по каждому участку

dvor1 <- dvor %>% add_count(Широта, Долгота)
# dvor1 %>% select(Широта, Долгота,n)

# Создаём переменную level. Используем переменную Подкатегория
# для оценки критерия и  присваиваем количественные значения для Подкатегоирй - всего 5.

dvor2 <- dvor1 %>% group_by(Подкатегория) %>%
        mutate(lev = as_factor(Подкатегория)) %>% 
        mutate(levels = as.numeric(lev))

# dvor2 %>% select(n, Название, levels)
dvor2 <- ungroup(dvor2)

#Создаём factor переменную {S,I, C} определяющую вид Критерия для каждой подкатегории.

# Критерии включают в себя следующие Названия:
# Безопасность [S] - {4}
# Физический комфорт, доступность [I] - {2,3,5}
# Комфорт восприятия органов чувcтв [C] - {1}

dvor3 <- dvor2 %>% group_by (Широта, Долгота, Название, levels) %>% 
        mutate(criteria = ifelse(levels == "4","S",
                                 ifelse(levels == "2"|levels == "3"|levels == "5", "I",
                                        ifelse(levels == "1","C", "no"))))


# dvor3 %>% group_by (Широта, Долгота,criteria) %>% select(Широта, Долгота, n, Подкатегория, Название, levels,criteria) 
dvor3 <- ungroup (dvor3)

# Определяем количество жалоб по каждой Подкатегории (by levels)
# n - количество всех жалоб по данному объекту
# nn - количество жалоб по данному объекту определенной Подкатегории

dvor4 <- dvor3 %>% add_count(Широта, Долгота,n,criteria)
# dvor4%>% select(Широта, Долгота,levels, n, nn,criteria)

# Проверка 
# dvor4  %>% arrange(Широта) %>% filter(levels ==3) %>% select(Широта, Долгота,levels, n, nn,criteria)

# Новая переменная weight - вклад атрибута (подкатегории) в оценку (1/(1+nn))

dvor5 <- dvor4 %>% group_by(levels,criteria) %>%
        mutate(weight = 1 / (nn + 1))
# dvor5 %>% select(Подкатегория, n, nn, weight)

# Cчитаем среднее по каждому критерию
# Количество Подкатегорий внутри каждого критерия:
# S - 1
# I - 3
# C - 1 

# Безопасность
# Для каждого двора (Широта, долгота) делим сумму весов на количество подкатегорий внутри каждого критерия. 

safety_dvor <- dvor5 %>% filter(criteria == "S") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(S = sum(weight) / 1) %>% 
        select(Широта, Долгота, S)

# Физический комфорт

comf_dvor <- dvor5 %>% filter(criteria == "I") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(I = sum(weight) / 3) %>% 
        select(Широта, Долгота, I)
# Эстетика

est_dvor <- dvor5 %>% filter(criteria == "C") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(C = sum(weight) / 1) %>% 
        select(Широта, Долгота, C)


# Cоздать датасет Широта, Долгота, S, I, C, total_index

# Создать переменную "dvorzone" с уникальными сочетаниями "широта" и "долгота" 
# для поиска всех уникальных адресов

dvorzone <- dvor %>% distinct(Широта, Долгота)

safety_dvor <- ungroup(safety_dvor)
comf_dvor <- ungroup(comf_dvor)
est_dvor <- ungroup(est_dvor)

# Объединяем в один финальный датасет адреса и  оценки по трём критериям
dvorzone1 <- dvorzone %>% left_join(safety_dvor, by = c("Широта", "Долгота")) %>% 
        left_join(comf_dvor, by = c("Широта", "Долгота")) %>% 
        left_join(est_dvor, by = c("Широта", "Долгота"))

# dvorzone1 %>% group_by(Широта, Долгота) %>% select(S,I,C)

# Заменяем пропущенные значения на 1 (отсутвие жалоб - высшая оценка среды)
# Расчитываем общую оценку потребительских свойств (total_index)
dvorzone2 <- dvorzone1 %>% distinct(Широта, Долгота, S, I, C) %>% 
        replace(is.na(.), 1)  %>% 
        mutate(total_index = rowMeans(select(.,c("S","I","C"))))
# dvorzone2

# Оценка для всего полигона.
# Расчитываем среднюю оценку по каждому критерию

dvorzone_result <- dvorzone2 %>% 
        summarise_at(c("S", "I", "C", "total_index"), mean)
# dvorzone_result

# 7. РАСЧЁТ ОБЩЕЙ БЕЗОПАСНОСТИ, ДОСТУПНОСТЬ И КОМФОРТ ВЫДЕЛЕННОГО ПОЛИГОНА
# Расчет потребительских свойст территории (сумма всех индекстов)  - в total_index. 

result_comb <- rbind(poly_result, poly_uds_result, greenzone_result,
                     waterzone_result, mafzone_result, dvorzone_result)%>%
        summarise_at(c("S", "I", "C", "total_index"), mean, na.rm = TRUE)
# result_comb

# Записываем файл
write_csv(round(result_comb, digits=4), paste(substr(filename, 1, nchar(filename) - 4), '_output', '.csv', sep=''))