#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// Размер структуры: frequency 4 байта + duration 4 байта + explorationMode 1 байт + байты выравнивания памяти 3 байта = 12 байт
typedef struct {
    int frequency; 
    int duration;
    bool explorationMode;
} StimulatorParams;

BLEServer *pServer = NULL;
BLECharacteristic *pNotifyCharacteristic = NULL;
BLECharacteristic *pWriteCharacteristic = NULL;
bool deviceConnected = false;

// Функция для генерации случайных данных для StimulatorParams
StimulatorParams generateRandomParams() {
    StimulatorParams params;
    params.frequency = random(100, 1000);
    params.duration = random(10, 100);
    params.explorationMode = random(0, 2);
    return params;
}

// Коллбэк для обработки подключения и отключения устройства
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
        deviceConnected = false;
        // Повторный запуск вещания после отключения устройства
        BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
        pAdvertising->start();
    }
};

// Коллбэк для обработки записи в характеристику
class WriteCallback : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
        Serial.println("onWrite");
        uint8_t* data = pCharacteristic->getData();
        size_t dataLength = pCharacteristic->getValue().length();
        Serial.println(dataLength);
        Serial.println(sizeof(StimulatorParams));

        if (dataLength == sizeof(StimulatorParams)) {
            // Десериализация полученных от приложения данных
            StimulatorParams params;
            memcpy(&params, data, sizeof(StimulatorParams));
            
            // Вывод в консоль
            Serial.println("Received frequency: " + String(params.frequency));
            Serial.println("Received duration: " + String(params.duration));
            Serial.println("Received exploration mode: " + String(params.explorationMode));

            // Установка новых значений характеристики для уведомления приложения, что новые данные были получены
            pNotifyCharacteristic->setValue(data, sizeof(StimulatorParams));
            pNotifyCharacteristic->notify(); // Отправка уведомления
        }
    }
};

// Коллбэк для обработки чтения из характеристики
class ReadCallback : public BLECharacteristicCallbacks {
    void onRead(BLECharacteristic *pCharacteristic) {
        // Генерация случайных значений
        StimulatorParams params = generateRandomParams();
        pCharacteristic->setValue((uint8_t*)&params, sizeof(StimulatorParams));
    }
};

void initBLE() {
    BLEDevice::init("Stimulator");
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks()); // Установка коллбэка для отслеживания подключения и отключения устройства
    BLEService *pService = pServer->createService(BLEUUID("cfff0681-0b4d-4542-92bc-72aa9fb777d3"));

    // Характеристика для чтения и уведомлений
    pNotifyCharacteristic = pService->createCharacteristic(
                       BLEUUID("59e9b7d5-e9f2-4e66-9693-49d58736181b"),
                       BLECharacteristic::PROPERTY_READ |
                       BLECharacteristic::PROPERTY_NOTIFY
                     );
    pNotifyCharacteristic->addDescriptor(new BLE2902());
    pNotifyCharacteristic->setCallbacks(new ReadCallback()); // Установка коллбэка для чтения

    // Характеристика для записи
    pWriteCharacteristic = pService->createCharacteristic(
                       BLEUUID("c88c01a2-b45d-42b0-8173-272be8e72332"),
                       BLECharacteristic::PROPERTY_WRITE
                     );
    pWriteCharacteristic->setCallbacks(new WriteCallback());

    pService->start();
    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(BLEUUID("cfff0681-0b4d-4542-92bc-72aa9fb777d3"));
    pAdvertising->setScanResponse(true);
    pAdvertising->start();
}

void setup() {
    Serial.begin(115200);
    initBLE();
    randomSeed(analogRead(0)); // Инициализация генератора случайных чисел
}

void loop() {

}
