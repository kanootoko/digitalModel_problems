# МОДЕЛЬ ОЦЕНКИ ОБЪЕКТОВ ГОРОДСКОЙ СРЕДЫ 
# НА ОСНОВЕ ЖАЛОБ И ОБРАЩЕНИЙ НАСЕЛЕНИЯ НА ПОРТАЛЫ ОБЩЕСТВЕННОГО УЧАСТИЯ
# РАСЧЁТ ОЦЕНКИ ДЛЯ ОТДЕЛЬНОЙ СУЩНОСТИ

filename <- 'test.csv'

args = commandArgs(trailingOnly=TRUE)
if (length(args) > 0) {
    filename <- args[1]
}

library(tidyverse)
#library(dplyr)
library(readr)

# 1. ЗДАНИЕ
# Расчёт оценки состояния объектов городской среды для отдельного объекта - здания 

datascore<-read_csv(filename,col_types = cols_only(
    ID = col_double(),
    Название = col_character(),
    Широта = col_double(),
    Долгота = col_double(),
    Подкатегория = col_character(),
    Категория = col_character()))


# 1. Анализируем здания
houses <- datascore%>% filter(Категория == "Дом"| Категория == "Сооружение"|Категория == "Квартира")

# Выделяем уникальные здания из датасета, у которых совпадает широта и долгота.
# Считаем количество жалоб по каждому дому

house1 <- houses %>% add_count(Широта, Долгота)
#house1 %>% select(Широта, Долгота,n)

# Создаём переменную level: присваиваем количественные значения для Подкатегорий - всего 19.

house2 <- house1 %>% group_by(Подкатегория) %>%
    mutate(lev = as_factor(Подкатегория)) %>% 
    mutate(levels = as.numeric(lev))

#house2 %>% select(ID, Широта, Долгота, n, Подкатегория, levels)
house2 <- ungroup(house2)

#Создаём factor переменную {S,I, C} определяющую вид Критерия для каждой подкатегории.

# Критерии включают в себя следующие Подкатегории:
# Безопасность [S] - 16,18,10,7
# Физический комфорт, доступность [I] - 2, 1, 4, 6, 8, 9,11,12,13,17,19
# Комфорт восприятия органов чувcтв [C] - 3, 5, 14, 15

house3 <- house2 %>% group_by (Широта, Долгота, Подкатегория, levels) %>% 
        mutate(criteria = ifelse(levels == "7"| levels == "10"| levels == "16"| levels == "18","S",
                         ifelse(levels == "2"| levels == "1"|levels == "4"| levels == "6"|
                                        levels == "8"| levels == "9"| levels == "11"|
                                        levels == "12"|levels == "13"|levels == "17"|levels == "19", "I",
                                ifelse(levels == "3"|levels == "5"|levels == "14"|levels == "15","C", "no"))))

house3 %>% group_by (Широта, Долгота,criteria) %>% select(Широта, Долгота, n, Подкатегория, levels,criteria)
house3 <- ungroup (house3)

# Определяем количество жалоб по каждой Подкатегории (by levels)
# n - количество всех жалоб по данному дому
# nn - количество жалоб по данному дому определенной Подкатегории

house4 <- house3 %>% add_count(Широта, Долгота,n,levels) 
house4 %>% select(Широта, Долгота,levels, n, nn,criteria)
 
# Проверка 
#house4  %>% arrange(Широта) %>% filter(levels ==1) %>% select(Широта, Долгота,levels, n, nn,criteria)

# Новая переменная weight - вклад атрибута (подкатегории) в оценку (1/(1+nn))

house5 <-house4 %>% group_by(levels,criteria) %>%
        mutate(weight = 1 / (nn + 1))
house5 %>% select(Подкатегория, n, nn, weight)

# Cчитаем среднее по каждому критерию
# Количество Подкатегорий внутри каждого критерия:
# S - 4
# I - 11
# C - 4 

# Безопасность
# Для каждого дома (Широта, долгота) делим сумму весов на 4. 

safety <- house5 %>% filter(criteria == "S") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(S = sum(weight) / 4) %>% 
        select(Широта, Долгота, S)
 
# Физический комфорт

comf <- house5 %>% filter(criteria == "I") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(I = sum(weight) / 11) %>% 
        select(Широта, Долгота, I)

# Эстетика

est <- house5 %>% filter(criteria == "C") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(C = sum(weight) / 4) %>% 
        select(Широта, Долгота, C)

# Cоздать датасет Широта, Долгота, S, I, C, total_index: 30,275 obs 6 vars
#     C      I      S 
# 101741 181733  25335 
# NA = 1

# Создать переменную "Дом" с уникальными сочетаниями "широта" и "долгота" 
# для поиска всех уникальных адресов

dom <- houses %>% distinct(Широта, Долгота)

safety <- ungroup(safety)
comf <- ungroup(comf)
est <- ungroup(est)

# Объединяем в один финальный датасет адреса и  оценки по трём критериям
dom1 <- dom %>% left_join(safety, by = c("Широта", "Долгота")) %>% 
        left_join(comf, by = c("Широта", "Долгота")) %>%
        left_join(est, by = c("Широта", "Долгота"))
#dom1 %>% group_by(Широта, Долгота) %>% 
    #    select(S,I,C)

# Заменяем пропущенные значения на 1 (отсутвие жалоб - высшая оценка среды)
# Расчитываем общую оценку потребительских свойств (total_index)
dom2 <- dom1 %>% distinct(Широта, Долгота, S, I, C) %>% 
        replace(is.na(.), 1) %>% 
        mutate(total_index = rowMeans(select(.,c("S","I","C"))))
dom2
# Записываем файл
#write_csv(dom2, "houses_evaluation.csv")
write_csv(round(dom2, digits=4), paste(substr(filename, 1, nchar(filename) - 4), '_output', '.csv', sep=''))

## 2. ДВОР.
# Расчёт оценки состояния объектов городской среды для отдельного объекта - Двор

# Включаем категории, характеризующие Зеленые зоны
dvor <- datascore %>% filter(Категория == "Двор")

# Выделяем уникальные участки из датасета, у которых совпадает широта и долгота.
# Считаем количество жалоб по каждому участку

dvor1 <- dvor %>% add_count(Широта, Долгота)
dvor1 %>% select(Широта, Долгота,n)

# Создаём переменную level. Используем переменную Подкатегория
# для оценки критерия и  присваиваем количественные значения для Подкатегоирй - всего 5.

dvor2 <- dvor1 %>% group_by(Подкатегория) %>%
        mutate(lev = as_factor(Подкатегория)) %>% 
        mutate(levels = as.numeric(lev))

dvor2 %>% select(n, Название, levels)
dvor2 <- ungroup(dvor2)

#Создаём factor переменную {S,I, C} определяющую вид Критерия для каждой подкатегории.

# Критерии включают в себя следующие Названия:
# Безопасность [S] - 4
# Физический комфорт, доступность [I] - 2,3,5
# Комфорт восприятия органов чувcтв [C] - 1

dvor3 <-dvor2 %>% group_by (Широта, Долгота, Название, levels) %>% 
        mutate(criteria = ifelse(levels == "4","S",
                                 ifelse(levels == "2"|levels == "3"|levels == "5", "I",
                                        ifelse(levels == "1","C", "no"))))


dvor3 %>% group_by (Широта, Долгота,criteria) %>% select(Широта, Долгота, n, Подкатегория, Название, levels,criteria) 
dvor3 <- ungroup (dvor3)

# Определяем количество жалоб по каждой Подкатегории (by levels)
# n - количество всех жалоб по данному объекту
# nn - количество жалоб по данному объекту определенной Подкатегории

dvor4 <- dvor3 %>% add_count(Широта, Долгота,n,criteria)
dvor4%>% select(Широта, Долгота,levels, n, nn,criteria)

# Проверка 
dvor4  %>% arrange(Широта) %>% filter(levels ==3) %>% select(Широта, Долгота,levels, n, nn,criteria)

# Новая переменная weight - вклад атрибута (подкатегории) в оценку (1/(1+nn))

dvor5 <-dvor4 %>% group_by(levels,criteria) %>%
        mutate(weight = 1 / (nn + 1))
dvor5 %>% select(Подкатегория, n, nn, weight)

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

dvorzone<- dvor %>% distinct(Широта, Долгота)

safety_dvor <- ungroup(safety_dvor)
comf_dvor <- ungroup(comf_dvor)
est_dvor <- ungroup(est_dvor)
# Объединяем в один финальный датасет адреса и  оценки по трём критериям
dvorzone1 <- dvorzone %>% left_join(safety_dvor, by = c("Широта", "Долгота")) %>% 
        left_join(comf_dvor, by = c("Широта", "Долгота")) %>% 
        left_join(est_dvor, by = c("Широта", "Долгота"))

dvorzone1 %>% group_by(Широта, Долгота) %>% 
        select(S,I,C)

# Заменяем пропущенные значения на 1 (отсутвие жалоб - высшая оценка среды)
# Расчитываем общую оценку потребительских свойств (total_index)
dvorzone2 <- dvorzone1 %>% distinct(Широта, Долгота, S, I, C) %>% 
        replace(is.na(.), 1)  %>% 
        mutate(total_index = rowMeans(select(.,c("S","I","C"))))
dvorzone2
# Записываем файл
#write_csv(dvorzone2, "dvor_evaluation.csv")
write_csv(round(dvorzone2, digits=4), paste(substr(filename, 1, nchar(filename) - 4), '_output', '.csv', sep=''))

# 3. МАФ 
# Расчёт оценки состояния объектов городской среды для отдельного объекта - МАФ

# Включаем категории, характеризующие Зеленые зоны
maf <- datascore %>% filter(Категория == "Остановка общественного транспорта" | Категория == "Временное сооружение")

# Выделяем уникальные участки из датасета, у которых совпадает широта и долгота.
# Считаем количество жалоб по каждому участку

maf1 <- maf %>% add_count(Широта, Долгота)
maf1 %>% select(Широта, Долгота,n)

# Создаём переменную level. Так как подкатегория всего одна, используем переменную
# Название для оценки критерия и  присваиваем количественные значения для Названий - всего 15.

maf2 <- maf1 %>% group_by(Название) %>%
        mutate(lev = as_factor(Название)) %>% 
        mutate(levels = as.numeric(lev))

maf2 %>% select(n, Название, levels)
maf2 <- ungroup(maf2)

#Создаём factor переменную {S,I, C} определяющую вид Критерия для каждой подкатегории.

# Критерии включают в себя следующие Названия:
# Безопасность [S] - 1,2
# Физический комфорт, доступность [I] - 3
# Комфорт восприятия органов чувcтв [C] - 3

maf3 <-maf2 %>% group_by (Широта, Долгота, Название, levels) %>% 
        mutate(criteria = ifelse(levels == "3"|levels == "2","S",
                                 ifelse(levels == "1" , "I", "no" )))

maf3 %>% group_by (Широта, Долгота,criteria) %>% select(Широта, Долгота, n, Название, levels,criteria) 
maf3 <- ungroup (maf3)

# Определяем количество жалоб по каждой Подкатегории (by levels)
# n - количество всех жалоб по данному объекту
# nn - количество жалоб по данному объекту определенной Подкатегории

maf4 <- maf3 %>% add_count(Широта, Долгота,n,criteria)
maf4%>% select(Широта, Долгота,levels, n, nn,criteria)

# Проверка 
maf4  %>% arrange(Широта) %>% filter(levels ==3) %>% select(Широта, Долгота,levels, n, nn,criteria)

# Новая переменная weight - вклад атрибута (подкатегории) в оценку (1/(1+nn))

maf5 <-maf4 %>% group_by(levels,criteria) %>%
        mutate(weight = 1 / (nn + 1))
maf5 %>% select(Подкатегория, n, nn, weight)

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

# Эстетика


# Cоздать датасет Широта, Долгота, S, I, C, total_index

# Создать переменную "mafzone" с уникальными сочетаниями "широта" и "долгота" 
# для поиска всех уникальных адресов

mafzone<- maf %>% distinct(Широта, Долгота)

safety_maf <- ungroup(safety_maf)
comf_maf <- ungroup(comf_maf)

# Объединяем в один финальный датасет адреса и  оценки по трём критериям
mafzone1 <- mafzone %>% left_join(safety_maf, by = c("Широта", "Долгота")) %>% 
        left_join(comf_maf, by = c("Широта", "Долгота")) 

mafzone1 %>% group_by(Широта, Долгота) %>% 
        select(S,I)

# Заменяем пропущенные значения на 1 (отсутвие жалоб - высшая оценка среды)
# Расчитываем общую оценку потребительских свойств (total_index)
mafzone2 <- mafzone1 %>% distinct(Широта, Долгота, S, I) %>% 
        replace(is.na(.), 1) %>% mutate(C = 1) %>% 
        mutate(total_index = rowMeans(select(.,c("S","I","C"))))
mafzone2
# Записываем файл
#write_csv(mafzone2, "maf_evaluation.csv")
write_csv(round(mafzone2, digits=4), paste(substr(filename, 1, nchar(filename) - 4), '_output', '.csv', sep=''))

# 4. ВОДНЫЕ ОБЪЕКТЫ
# Расчёт оценки состояния объектов городской среды для отдельного объекта - УДС

# Включаем категории, характеризующие Водные объекты
water <- datascore %>% filter(Категория == "Водный объект")

# Выделяем уникальные участки из датасета, у которых совпадает широта и долгота.
# Считаем количество жалоб по каждому участку

water1 <- water %>% add_count(Широта, Долгота)
water1 %>% select(Широта, Долгота,n)

# Создаём переменную level. Так как подкатегория и название всего одно - "Мусор в воде или на берегу водного объекта"
# мождно оценить только один критерий Комфорт восприятия органов чувcтв [C], и на основе его вывести общий критерий

water2 <- water1 %>% group_by(Название) %>%
        mutate(lev = as_factor(Название)) %>% 
        mutate(levels = as.numeric(lev))

water2 %>% select(n, Название, levels)
water2 <- ungroup(water2)

#Создаём factor переменную {S,I, C} определяющую вид Критерия для каждой подкатегории.

# Критерии включают в себя следующие Названия:
# Безопасность [S] - 1,14
# Физический комфорт, доступность [I] - 3,7,8,11,12,15
# Комфорт восприятия органов чувcтв [C] - 2,4,5,6,9,10,13

water3 <-water2 %>% group_by (Широта, Долгота, Название, levels) %>% 
        mutate(criteria = ifelse(levels == "1","С","no"))
water3 <- ungroup (water3)

# Определяем количество жалоб по каждой Подкатегории (by levels)
# n - количество всех жалоб по данному объекту
# nn - количество жалоб по данному объекту определенной Подкатегории

water4 <- water3 %>% add_count(Широта, Долгота,n,criteria)

# Новая переменная weight - вклад атрибута (подкатегории) в оценку (1/(1+nn))

water5 <-water4 %>% group_by(levels,criteria) %>%
        mutate(weight = 1 / (nn + 1))
water5 %>% select(Подкатегория, n, nn, weight)

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

waterzone<- water %>% distinct(Широта, Долгота)
est_water <- ungroup(est_water)

# Объединяем в один финальный датасет адреса и  оценки по трём критериям
waterzone1 <- waterzone %>% left_join(est_water, by = c("Широта", "Долгота"))
waterzone1 %>% group_by(Широта, Долгота) %>% 
        select(C)

# Заменяем пропущенные значения на 1 (отсутвие жалоб - высшая оценка среды)
# Расчитываем общую оценку потребительских свойств (total_index)
waterzone2 <- waterzone1 %>% distinct(Широта, Долгота, C) %>% 
        mutate (S = 1) %>% mutate(I = 1) %>% 
        replace(is.na(.), 1) %>% 
        mutate(total_index = rowMeans(select(.,c("S","I","C")))) %>% select(Широта, Долгота, S,I,C, total_index)
waterzone2
# Записываем файл
#write_csv(waterzone2, "water_evaluation.csv")
write_csv(round(waterzone2, digits=4), paste(substr(filename, 1, nchar(filename) - 4), '_output', '.csv', sep=''))


# 5. ЗЕЛЕНЫЕ ЗОНЫ
# Расчёт оценки состояния объектов городской среды для отдельного объекта - Зеленые зоны

# Включаем категории, характеризующие Зеленые зоны
green <- datascore %>% filter(Категория == "Парк, сад, бульвар, сквер" | Категория == "Кладбище")

# Выделяем уникальные участки из датасета, у которых совпадает широта и долгота.
# Считаем количество жалоб по каждому участку

green1 <- green %>% add_count(Широта, Долгота)
green1 %>% select(Широта, Долгота,n)

# Создаём переменную level. Так как подкатегория всего одна, используем переменную
# Название для оценки критерия и  присваиваем количественные значения для Названий - всего 15.

green2 <- green1 %>% group_by(Название) %>%
        mutate(lev = as_factor(Название)) %>% 
        mutate(levels = as.numeric(lev))

green2 %>% select(n, Название, levels)
green2 <- ungroup(green2)

#Создаём factor переменную {S,I, C} определяющую вид Критерия для каждой подкатегории.

# Критерии включают в себя следующие Названия:
# Безопасность [S] - 1,14
# Физический комфорт, доступность [I] - 3,7,8,11,12,15
# Комфорт восприятия органов чувcтв [C] - 2,4,5,6,9,10,13

green3 <-green2 %>% group_by (Широта, Долгота, Название, levels) %>% 
        mutate(criteria = ifelse(levels == "1"| levels == "14","S",
                                 ifelse(levels == "3"|levels == "7"| levels == "8"| levels == "11"| levels == "12"| levels == "15", "I",
                                        ifelse(levels == "2"|levels == "4"| levels == "5"| levels == "6"| levels == "9"|
                                                       levels == "10"| levels == "13","C", "no"))))

green3 %>% group_by (Широта, Долгота,criteria) %>% select(Широта, Долгота, n, Название, levels,criteria) 
green3 <- ungroup (green3)

# Определяем количество жалоб по каждой Подкатегории (by levels)
# n - количество всех жалоб по данному объекту
# nn - количество жалоб по данному объекту определенной Подкатегории

green4 <- green3 %>% add_count(Широта, Долгота,n,criteria)
green4%>% select(Широта, Долгота,levels, n, nn,criteria)

# Проверка 
green4  %>% arrange(Широта) %>% filter(levels ==3) %>% select(Широта, Долгота,levels, n, nn,criteria)

# Новая переменная weight - вклад атрибута (подкатегории) в оценку (1/(1+nn))

green5 <-green4 %>% group_by(levels,criteria) %>%
        mutate(weight = 1 / (nn + 1))
green5 %>% select(Подкатегория, n, nn, weight)

# Cчитаем среднее по каждому критерию
# Количество Подкатегорий внутри каждого критерия:
# S - 2
# I - 6
# C - 7 

# Безопасность
# Для каждой зеленой зоны (Широта, долгота) делим сумму весов на количество подкатегорий внутри каждого критерия. 

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



# Создать переменную "greenzone" с уникальными сочетаниями "широта" и "долгота" 
# для поиска всех уникальных адресов

greenzone<- green %>% distinct(Широта, Долгота)

safety_green <- ungroup(safety_green)
comf_green <- ungroup(comf_green)
est_green <- ungroup(est_green)

# Объединяем в один финальный датасет адреса и  оценки по трём критериям
greenzone1 <- greenzone %>% left_join(safety_green, by = c("Широта", "Долгота")) %>% 
        left_join(comf_green, by = c("Широта", "Долгота")) %>%
        left_join(est_green, by = c("Широта", "Долгота"))
greenzone1 %>% group_by(Широта, Долгота) %>% 
        select(S,I,C)

# Заменяем пропущенные значения на 1 (отсутвие жалоб - высшая оценка среды)
# Расчитываем общую оценку потребительских свойств (total_index)
greenzone2 <- greenzone1 %>% distinct(Широта, Долгота, S, I, C) %>% 
        replace(is.na(.), 1) %>% 
        mutate(total_index = rowMeans(select(.,c("S","I","C"))))
greenzone2
# Записываем файл
#write_csv(greenzone2, "green_evaluation.csv")
write_csv(round(greenzone2, digits=4), paste(substr(filename, 1, nchar(filename) - 4), '_output', '.csv', sep=''))

# 6. УДС
# Расчёт оценки состояния объектов городской среды для отдельного объекта - УДС

# Включаем категории, характеризующие УДС
uds <- datascore %>% filter(Категория == "Мост" | Категория == "Улица" |
                                             Категория == "Общественный транспорт"|
                                             Категория == "Территория Санкт-Петербурга") %>% 
        filter(Подкатегория == "Благоустройство"|
                                   Подкатегория == "Повреждения или неисправность элементов уличной инфраструктуры"|
                                   Подкатегория == "Техническое и санитарное состояние транспортных средств")

# Выделяем уникальные участки из датасета, у которых совпадает широта и долгота.
# Считаем количество жалоб по каждому участку

uds1 <- uds %>% add_count(Широта, Долгота)
uds1 %>% select(Широта, Долгота,n)

# Создаём переменную level: присваиваем количественные значения для Подкатегорий - всего 3.

uds2 <- uds1 %>% group_by(Подкатегория) %>%
        mutate(lev = as_factor(Подкатегория)) %>% 
        mutate(levels = as.numeric(lev))

uds2 %>% select(ID, Широта, Долгота, n, Подкатегория, levels)
uds2 <- ungroup(uds2)

#Создаём factor переменную {S,I, C} определяющую вид Критерия для каждой подкатегории.

# Критерии включают в себя следующие Подкатегории:
# Безопасность [S] - {3}
# Физический комфорт, доступность [I] - {2}
# Комфорт восприятия органов чувcтв [C] - {1}

uds3 <-uds2 %>% group_by (Широта, Долгота, Подкатегория, levels) %>% 
        mutate(criteria = ifelse(levels == "3","S",
                                 ifelse(levels == "2", "I",
                                        ifelse(levels == "1","C", "no"))))

uds3 %>% group_by (Широта, Долгота,criteria) %>% select(Широта, Долгота, n, Подкатегория, levels,criteria) 
uds3 <- ungroup (uds3)

# Определяем количество жалоб по каждой Подкатегории (by levels)
# n - количество всех жалоб по данному объекту
# nn - количество жалоб по данному объекту определенной Подкатегории

uds4 <- uds3 %>% add_count(Широта, Долгота,n,criteria)
uds4%>% select(Широта, Долгота,levels, n, nn,criteria)

# Проверка 
uds4  %>% arrange(Широта) %>% filter(levels ==3) %>% select(Широта, Долгота,levels, n, nn,criteria)

# Новая переменная weight - вклад атрибута (подкатегории) в оценку (1/(1+nn))

uds5 <-uds4 %>% group_by(levels,criteria) %>%
        mutate(weight = 1 / (nn + 1))
uds5 %>% select(Подкатегория, n, nn, weight)

# Cчитаем среднее по каждому критерию
# Количество Подкатегорий внутри каждого критерия:
# S - 1
# I - 1
# C - 1 

# Безопасность
# Для каждого участка УДС (Широта, долгота) делим сумму весов на количество подкатегорий внутри каждого критерия. 

safety_uds <- uds5 %>% filter(criteria == "S") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(S = sum(weight) / 1) %>% 
        select(Широта, Долгота, S)

# Физический комфорт

comf_uds <- uds5 %>% filter(criteria == "I") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(I = sum(weight) / 1) %>% 
        select(Широта, Долгота, I)

# Эстетика

est_uds <- uds5 %>% filter(criteria == "C") %>% 
        group_by(Широта, Долгота, criteria) %>% 
        mutate(C = sum(weight) / 1) %>% 
        select(Широта, Долгота, C)


# Создать переменную "road" с уникальными сочетаниями "широта" и "долгота" 
# для поиска всех уникальных адресов

road<- uds %>% distinct(Широта, Долгота)

safety_uds <- ungroup(safety_uds)
comf_uds <- ungroup(comf_uds)
est_uds <- ungroup(est_uds)

# Объединяем в один финальный датасет адреса и  оценки по трём критериям
road1 <- road %>% left_join(safety_uds, by = c("Широта", "Долгота")) %>% 
        left_join(comf_uds, by = c("Широта", "Долгота")) %>%
        left_join(est_uds, by = c("Широта", "Долгота"))
road1 %>% group_by(Широта, Долгота) %>% 
        select(S,I,C)

# Заменяем пропущенные значения на 1 (отсутвие жалоб - высшая оценка среды)
# Расчитываем общую оценку потребительских свойств (total_index)
road2 <- road1 %>% distinct(Широта, Долгота, S, I, C) %>% 
        replace(is.na(.), 1) %>% 
        mutate(total_index = rowMeans(select(.,c("S","I","C"))))
road2
# Записываем файл
#write_csv(road2, "uds_evaluation.csv")
write_csv(round(road2, digits=4), paste(substr(filename, 1, nchar(filename) - 4), '_output', '.csv', sep=''))


