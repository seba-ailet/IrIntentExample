# Взаимодействие через Интенты

Позволяет использовать приложение IR без интеграции библиотеки, достаточно что бы приложение IR было устновлено на устройстве.

- [Взаимодействие через Интенты](#взаимодействие-через-интенты)
  - [Вызов](#вызов)
    - [Методы](#методы)
    - [Параметры вызова](#параметры-вызова)
    - [Пример вызова метода](#пример-вызова-метода)
  - [Ответ](#ответ)
    - [Формат поля json](#формат-поля-json)
    - [Пример поля json](#пример-поля-json)
    - [Статусы](#статусы)
    - [Пример обработки ответа](#пример-обработки-ответа)
  - [Broadcast-сообщение](#broadcast-сообщение)
    - [Содержимое broadcast-сообщения](#содержимое-broadcast-сообщения)
    - [Пример обработки broadcast-сообщения](#пример-обработки-broadcast-сообщения)
  - [Возможные проблемы при интеграции](#возможные-проблемы-при-интеграции)
    - [Особенности Android 11](#особенности-android-11)

## Вызов

### Методы

Метод  | Описание
------------- | -------------
visit  | Создание/редактирование визита (activity)
report | Отчет по визиту (json)
summaryReport | Сводный отчет по визиту (activity)
sync | Запуск фонового процесса передачи фото и получения результатов

### Параметры вызова

Поле  | Описание | Обязательно для методов
------------- | ------------- | -------------
method  | Метод | для всех
login | Логин пользователя | для всех
password | Пароль пользователя | для всех
id | ИД пользователя | для всех, если используется технический пользователь
visit_id | ИД визита | visit, report, summaryReport
store_id | ИД торговой точки | visit

### Пример вызова метода

```java
Intent intent = getPackageManager().getLaunchIntentForPackage("com.intelligenceretail.www.pilot");
                if (intent != null) {
                    intent.setAction(Intent.ACTION_RUN);
                    intent.setFlags(0);
                    intent.putExtra("method", "visit");
                    intent.putExtra("login", user);
                    intent.putExtra("password", password);
                    intent.putExtra("id", user_id);
                    intent.putExtra("visit_id", visit_id);
                    intent.putExtra("store_id", store_id);
                    intent.putExtra("task_id", task_id);
                    startActivityForResult(intent, ACTIVITY_RESULT_START_IR_VISIT);
                }
```

## Ответ

Для возврата результата используется FileProvider, intent в атрибуте data содержит Uri файла с данными.

Поле  | Описание
------------- | -------------
error  | Ошибка, при resultCode == RESULT_CANCELED
data | Uri с файлом результата операции

### Формат поля json

Поле  | Описание | Наличие в ответе
------------- | ------------- | -------------
photosCounter  | Количество сделанных фото | при status != ошибке
scenesCounter  | Количество сцен | при status != ошибке
notDetectedPhotosCounter  | Количество фото, по которым не получены данные | при status != ошибке
notDetectedScenesCounter  | Количество сцен, по которым не получены данные | при status != ошибке
nonValidPhotosCounter  | Количество некачественных и не принятых пользователем фото | при status != ошибке
report  | Отчет (формат отчета в документации) | при status == RESULT_OK
status  | Статус выполнения метода | всегда

### Пример поля json

```json
{
    "photosCounter": 1,
    "scenesCounter": 1,
    "notDetectedPhotosCounter": 0,
    "notDetectedScenesCounter": 0,
    "nonValidPhotosCounter": 0,
    "report": {...},
    "status": "RESULT_OK"
}
```

### Статусы 

Статус  | Описание
------------- | -------------
RESULT_OK | Успешно
ERROR_BUSY | Уже выполнятеся
RESULT_EMPTY | Пустой отчет
RESULT_INPROGRESS | Данные в обработке
RESULT_INPROGRESS_OFFLINE | Данные в обработке (приложение в режиме оффлайн)
ERROR | Ошибка
ERROR_TOKEN | Ошибка токена
ERROR_STORE_ID_INCORRECT | Некорректный ИД торновой точки
ERROR_VISIT_ID_INCORRECT | Неорректный ИД визита
ERROR_NOVISIT | Визита не существует
ERROR_STORES_EMPTY | Справочник торговых точек пустой
ERROR_INCORRECT_INPUT_PARAMS | Неверные входные параметры
ERROR_INCORRECT_METHOD | Неверный метод
ERROR_AUTH | Ошибка авторизации
ERROR_NO_INET | Отсутствие интернета

### Пример обработки ответа

```java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (data.getData() != null) {
                String result = readFromUri(data.getData());                
                try {
                    JSONObject json = new JSONObject(result);                    
                    addlog(json.toString());
                    Toast.makeText(getBaseContext(), mode + " " + json.getString("status"), Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (data.getExtras() != null)
                Toast.makeText(getBaseContext(), "ERROR_ACTIVITY_RESULT " + data.getExtras().getString("error"), Toast.LENGTH_LONG).show();
        }
    }

    private String readFromUri(Uri uri){
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            InputStreamReader isReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(isReader);
            StringBuffer sb = new StringBuffer();
            String str;
            while((str = reader.readLine())!= null){
                sb.append(str);
            }

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }
```

## Broadcast-сообщение

При вызове метода visit и создании фото запускается фоновый процесс передачи фото и получения отчетов, который по завершении формирует broadcast сообщение IR_BROADCAST_SHARESHELF.

### Содержимое broadcast-сообщения

Поле  | Описание
------------- | -------------
VISIT_ID  | Внутренний ИД визита
EXTERNAL_VISIT_ID | ИД визита
json | Ответ в [формате json](#%d0%a4%d0%be%d1%80%d0%bc%d0%b0%d1%82-%d0%bf%d0%be%d0%bb%d1%8f-json)

### Пример обработки broadcast-сообщения

```java
shareShelfBroadcast = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            try {
                JSONObject json = new JSONObject(extras.getString("json"));
                Toast.makeText(getBaseContext(), "BROADCAST_SHARESHELF " + json.getString("status"), Toast.LENGTH_LONG).show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
};

this.registerReceiver(shareShelfBroadcast, new IntentFilter("IR_BROADCAST_SHARESHELF"));
```

## Возможные проблемы при интеграции

### Особенности Android 11

Если в проекте используется targetSdkVersion 30, то может возникнуть проблема с вызовом приложение IR, для ее решения есть несколько сопособов:

- добавить queries в AndroidManifest (предпочтительный вариант)

    ```xml
    <queries>
        <package android:name="com.intelligenceretail.www.pilot" />
    </queries>
    ```

- добавить queries в AndroidManifest 

    ```xml
    <queries>
        <intent>
            <action android:name="android.intent.action.VIEW" />
        </intent>
        <intent>
            <action android:name="android.intent.action.RUN" />
        </intent>
    </queries>
    ```

- добавить QUERY_ALL_PACKAGES в AndroidManifest 
  
    ```xml
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>
    ```

- понизить targetSdkVersion до версии 29
