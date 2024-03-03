# Electrostimulator
Electrostimulator - это Android-приложение, созданное для взаимодействия с устройством электростимулятора посредством Bluetooth Low Energy (BLE). Пользователи могут легко устанавливать соединение с устройством и редактировать его параметры непосредственно через приложение. После успешного внесения изменений приложение сообщает об этом пользователю. Кроме того, оно надежно обрабатывает все возможные ошибки, связанные с установлением и разрывом связи по Bluetooth.
Дополнительно, в проекте представлен скетч на языке C, расположенный в корневом каталоге, который может быть использован для прошивки микроконтроллера ESP32 для тестирования данного приложения.

## Использованные технологии:
Язык программирования: Kotlin
Архитектурный паттерн: MVVM (Model-View-ViewModel)
Библиотеки:
Встроенные библиотеки для работы с BLE
ViewModel и LiveData для реализации архитектурного паттерна MVVM
Navigation Component для реализации навигации
Material Components для дизайна пользовательского интерфейса
Разметка: XML

## Скриншоты и видео 
<img src="screenshots/1.gif" alt="Запуск поиск и подключение" width=240> <img src="screenshots/2.gif" alt="Смена параметров 1" width=240> <img src="screenshots/3.gif" alt="Смена параметров 2" width=240>

<img src="screenshots/4.gif" alt="Реакция приложения на потерю сигнала" width=240>

## Установка: 
1. Клонировать репозиторий.
2. Открыть проект в Android Studio.
3. Собрать и запустить приложение на Android-устройстве.
4. Открыть скетч в среде Arduino и убедиться, что установлены зависимости для ESP32 и BLE.
5. Загрузить прошивку на микроконтроллер ESP32.
После проделывания всех шагов устройство должно отображаться в Android-приложении и быть доступным для подключения. 
