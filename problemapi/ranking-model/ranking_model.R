# МОДЕЛЬ ОЦЕНКИ ОБЪЕКТОВ ГОРОДСКОЙ СРЕДЫ 
# НА ОСНОВЕ ЖАЛОБ И ОБРАЩЕНИЙ НАСЕЛЕНИЯ НА ПОРТАЛЫ ОБЩЕСТВЕННОГО УЧАСТИЯ

filename <- 'test.csv'

args = commandArgs(trailingOnly=TRUE)
if (length(args) > 0) {
        filename <- args[1]
}


# Расчёт оценки состояния объектов городской среды (зданий) внутри полигона

library(tidyverse)

# Загружаем файл только с сущностями "дом", сформированный из общего датасета "problems_exposrt_2020-05-27' 
houses <- read_csv(filename, col_names = TRUE,
                   cols(ID = col_double(),
                        Название = col_character(),
                        Широта = col_double(),
                        Долгота = col_double(),
                        Подкатегория = col_factor(),
                        Категория = col_factor()    
                   ))

poly <- tibble(houses)

# Считаем количество жалоб по каждому дому

poly1 <- poly %>% add_count(Широта, Долгота)

# Создаём переменную level: присваиваем количественные значения для Подкатегорий - всего 19.

poly2 <- poly1 %>% group_by(Подкатегория) %>%
        mutate(levels = as.numeric(Подкатегория))
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

poly3 <- ungroup (poly3)

# Определяем количество жалоб по каждой Подкатегории (by levels)
# n - количество всех жалоб по данному дому
# nn - количество жалоб по данному дому определенной Подкатегории

poly4 <- poly3 %>% add_count(n,levels) 

# Новая переменная weight - вклад атрибута (подкатегории) в оценку (1/(1+nn))

poly5 <-poly4 %>% group_by(levels,criteria) %>%
        mutate(weight = 1 / (nn + 1))

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
# Создать переменную "Дом"(build) с уникальными сочетаниями "широта" и "долгота" 
# для поиска всех уникальных адресов

build <- poly %>% distinct(Широта, Долгота)

safety_poly <- ungroup(safety_poly)
comf_poly <- ungroup(comf_poly)
est_poly <- ungroup(est_poly)

# Объединяем в один финальный датасет адреса и  оценки по трём критериям

build1 <- build %>% left_join(safety_poly, by = c("Широта", "Долгота")) %>% 
        left_join(comf_poly, by = c("Широта", "Долгота")) %>%
        left_join(est_poly, by = c("Широта", "Долгота"))

# Заменяем пропущенные значения на 1 (отсутвие жалоб - высшая оценка среды)
# Расчитываем общую оценку потребительских свойств (total_index)
build2 <- build1 %>% distinct(Широта, Долгота, S, I, C) %>% 
        replace(is.na(.), 1) %>% 
        mutate(total_index = rowMeans(select(.,c("S","I","C"))))

# Оценка для всего полигона.
# Расчитываем среднюю оценку по каждому критерию

poly_result <- build2 %>% 
        summarise_at(c("S", "I", "C", "total_index"), mean)

# Записываем файл
write_csv(round(poly_result, digits=4), paste(substr(filename, 1, nchar(filename) - 4), '_output', '.csv', sep=''))
