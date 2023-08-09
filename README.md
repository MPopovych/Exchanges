# About

This is a library is use for personal purposes as a **git submodule**.

Written in pure Kotlin, built on gradle 8.1.1

## Public api support

| Exchange | Market info | OrderBook | Kline | Ticker |
|:--------:|:-----------:|:---------:|:-----:|:------:|
| Binance  |      +      |     -     |   +   |   -    |
|    -     |      -      |     -     |   -   |   -    |

## Private api support

| Exchange | Order (limit) | Order info | Order cancel | All open orders |
|:--------:|:-------------:|:----------:|:------------:|:---------------:|
| Binance  |       +       |     +      |      +       |        +        |
|    -     |       -       |     -      |      -       |        -        |

## WebSocket support

| Exchange | KLine | OrderBook | Public Trades | Private Trades |
|:--------:|:-----:|:---------:|:-------------:|:--------------:|
| Binance  |   +   |     -     |       -       |       -        |
|    -     |   -   |     -     |       -       |       -        |

### Plans

- OB sockets
- Migrate exchanges from old library (private) onto this one