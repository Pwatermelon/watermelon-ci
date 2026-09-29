import { STOREFRONT_KARBYZ } from './storefrontGuide'

export type KarbyzExample = {
  id: string
  title: string
  summary: string
  constructs: string[]
  source: string
  expected?: string
  pipeline?: boolean
}

export const KARBYZ_EXAMPLES: KarbyzExample[] = [
  {
    id: 'hello',
    title: 'Минимальный модуль',
    summary: 'Единица компиляции и вывод.',
    constructs: ['жыелма', 'чыгар'],
    source: `жыелма салам ::
  чыгар ("Карбыз работает!")
ахыр жыелма.`,
    expected: 'Карбыз работает!',
  },
  {
    id: 'all',
    title: 'Все основные конструкции',
    summary: 'Типы, функция, условие, while, for, массив, арифметика.',
    constructs: ['сан', 'суз', 'тезма', 'эшлама', 'агар', 'шулай', 'очен', 'озынлык'],
    source: `жыелма барлык_конструкциялар ::

  сан кабат <- 3
  суз исем <- "storefront"
  тезма хезмэтлар <- ["api", "ui", "db"]

  эшлама салам (суз кем) ::
    чыгар ("Привет,", кем)
    кайтар
  ахыр

  салам (исем)

  агар (кабат > 0) булса ::
    чыгар ("настройка идёт")
  юкса ::
    чыгар ("остановлено")
  ахыр

  сан i <- 0
  шулай (i < озынлык (хезмэтлар)) вакыт ::
    чыгар ("сервис:", хезмэтлар[i])
    i <- i + 1
  ахыр

  очен (сан j <- 0 да j < кабат кадак j <- j + 1) ::
    чыгар ("итерация", j)
  ахыр

  сан сумма <- 10 * 2 + 5
  чыгар ("сумма=", сумма)

ахыр жыелма.`,
    expected: `Привет, storefront
настройка идёт
сервис: api
сервис: ui
сервис: db
итерация 0
итерация 1
итерация 2
сумма= 25`,
  },
  {
    id: 'io',
    title: 'Ввод и вывод',
    summary: 'Операторы керт / чыгар.',
    constructs: ['керт', 'чыгар', 'сан', 'суз'],
    source: `жыелма керту_чыгару ::
  чыгар ("Введите число:")
  керт (сан n)
  чыгар ("удвоенное:", n * 2)

  чыгар ("Введите строку:")
  керт (суз s)
  чыгар ("вы ввели:", s)
ахыр жыелма.`,
    expected: 'Ввод: 7 и hello → удвоенное: 14 / вы ввели: hello',
  },
  {
    id: 'devops',
    title: 'Storefront · полный CI',
    summary: 'Git → build → test → registry → secrets → deploy на Карбызе.',
    constructs: ['этап', 'башкар', 'сервер', 'checkout', 'publish'],
    pipeline: true,
    source: STOREFRONT_KARBYZ,
    expected: 'Compile / Run → Jenkinsfile (нативный .kbz)',
  },
  {
    id: 'error',
    title: 'Синтаксическая ошибка',
    summary: 'Нет точки после «ахыр жыелма» — разбор обязан отказать.',
    constructs: ['диагностика', 'синтаксис'],
    source: `жыелма хата ::
  сан x <- 1
  чыгар (x)
ахыр жыелма`,
    expected: '[синтаксис] …: ожидалось DOT, получено EOF',
  },
]
