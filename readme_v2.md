# Взаимодействие через Интенты

Позволяет использовать приложение IR без интеграции библиотеки, достаточно что бы приложение IR было устновлено на устройстве.

- [Взаимодействие через Интенты](#взаимодействие-через-интенты)
  - [Вызов](#вызов)
    - [Методы](#методы)
    - [Параметры вызова](#параметры-вызова)
    - [Пример вызова метода](#пример-вызова-метода)
  - [Ответ](#ответ)
    - [Формат данных ответа](#формат-данных-ответа)
    - [Получение изображений из report](#получение-изображений-из-report)
    - [Статусы](#статусы)
    - [Пример обработки ответа](#пример-обработки-ответа)
  - [Broadcast-сообщение](#broadcast-сообщение)
    - [Содержимое broadcast-сообщения](#содержимое-broadcast-сообщения)
    - [Пример обработки broadcast-сообщения](#пример-обработки-broadcast-сообщения)
  - [Пример отчета (поле result в broadcast и getData() в onActivityResult)](#пример-отчета-поле-result-в-broadcast-и-getdata-в-onactivityresult)
  - [Пример взаимодействия](#пример-взаимодействия)
  - [Возможные проблемы при интеграции](#возможные-проблемы-при-интеграции)
    - [Особенности Android 11](#особенности-android-11)

## Вызов

### Методы

Метод  | Описание
------------- | -------------
com.intrtl.app.ACTION_VISIT  | Создание/редактирование визита (activity)
com.intrtl.app.ACTION_REPORT | Отчет по визиту (json)
com.intrtl.app.ACTION_SUMMARY_REPORT | Сводный отчет по визиту (activity)
com.intrtl.app.ACTION_SYNC | Запуск фонового процесса передачи фото и получения результатов

### Параметры вызова

Поле  | Описание | Обязательно для методов | Необязательно для методов
------------- | ------------- | ------------- | -------------
**Параметры**  | |
action  | Метод | 
**Extra**  | | 
login | Логин пользователя | для всех
password | Пароль пользователя | для всех
id | ИД пользователя | для всех, если используется технический пользователь
visit_id | ИД визита | visit, report, summaryReport
task_id | ИД задачи | | visit, report, summaryReport
store_id | ИД торговой точки | visit

### Пример вызова метода

```java
Intent intent = new Intent();
if (intent != null) {
    intent.setAction("com.intrtl.app.ACTION_VISIT");
    intent.setFlags(0);                    
    intent.putExtra("login", user);
    intent.putExtra("password", password);
    intent.putExtra("id", user_id);
    intent.putExtra("visit_id", visit_id);
    intent.putExtra("store_id", store_id);
    startActivityForResult(intent, ACTIVITY_RESULT_START_IR_VISIT);
}
```

## Ответ

Для возврата результата используется FileProvider, intent в атрибуте data содержит Uri файла с данными.

Поле  | Описание
------------- | -------------
error  | Ошибка, при resultCode == RESULT_CANCELED
data | Uri с файлом результата операции

### Формат данных ответа

[Пример](#пример-отчета-поле-result-в-broadcast-и-getdata-в-onactivityresult)

Поле  | Описание | Наличие в ответе
------------- | ------------- | -------------
status | Статус выполнения метода | всегда
user_id | Идентификатор пользователя | кроме метода ACTION_SYNC
store_id | Идентификатор магазина | кроме метода ACTION_SYNC
task_id | Идентификатор задачи | кроме метода ACTION_SYNC
visit_id | Идентификатор визита | кроме метода ACTION_SYNC
internal_visit_id | Внутренний идентификатор визита | кроме метода ACTION_SYNC
install_id | ИД установки | кроме метода ACTION_SYNC
photosCounter | Количество сделанных фото | при status != ERROR_VISIT_ID_INCORRECT и методе != ACTION_SYNC
scenesCounter | Количество сцен | при status != ERROR_VISIT_ID_INCORRECT и методе != ACTION_SYNC
notDetectedPhotosCounter | Количество фото, по которым не получены данные | при status != ERROR_VISIT_ID_INCORRECT и методе != ACTION_SYNC
notDetectedScenesCounter | Количество сцен, по которым не получены данные | при status != ERROR_VISIT_ID_INCORRECT и методе != ACTION_SYNC
report | Отчет | при status == RESULT_OK и методе != ACTION_SYNC

### Получение изображений из report

В новых версиях ОС Android (9 и новее) для получения пути файла изображения вместо image_path необходимо использовать image_uri. 
Пример получения изображение:

```kotlin
private fun readBitmapFromUri(uri: Uri): Bitmap? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(this.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    } else {
        MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
    }
}
```

```kotlin
val photosJSON = json.getJSONObject("report").getJSONObject("photos")
val photoNamesList = ArrayList<String>()
for ( i in 0 until photosJSON.length()) {
    photoNamesList.add(photosJSON.names()[i] as String)
}
val arrayOfBitmap = photoNamesList.map {
    val photoUri = Uri.parse(photosJSON.getString(it))
    readBitmapFromUri(photoUri)
}
```

### Статусы 

Статус  | Описание
------------- | -------------
RESULT_OK | Успешно
RESULT_INPROGRESS | Данные в обработке
RESULT_INPROGRESS_OFFLINE | Данные в обработке (приложение в режиме оффлайн)
RESULT_EMPTY | Пустой отчет
ERROR_NOVISIT | Визита не существует
ERROR_READONLY_VISIT | Визита только для чтения
ERROR_INCORRECT_INPUT_PARAMS | Неверные входные параметры
ERROR_VISIT_ID_INCORRECT | Неорректный ИД визита
ERROR_AUTH | Ошибка авторизации
ERROR_VISIT_ID_INCORRECT | Неорректный ИД визита
ERROR_PHOTO | Фото с ошибкой
ERROR_BUSY | Метод уже выполняется
ERROR_CANT_LOAD_VISIT | Невозможно загрузить визит, так как отсуствует интернет

### Пример обработки ответа

```java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            String mode = "";
            switch (requestCode) {

                case (ACTIVITY_RESULT_START_IR_REPORT):
                    mode = "reports";
                    break;

                case (ACTIVITY_RESULT_START_IR_VISIT):
                    mode = "visit";
                    break;

                case (ACTIVITY_RESULT_START_IR_SUMMARYREPORT):
                    mode = "summaryReport";
                    break;
            }

            if (data.getData() != null) {
                String result = readFromUri(data.getData());                
                try {
                    JSONObject json = new JSONObject(result);
                    Log.i("report", json.toString());                                        
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            //On Error
        }
    }

    private String readFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();
            String string;
            while ((string = reader.readLine()) != null) {
                stringBuffer.append(string);
            }
            reader.close();
            inputStreamReader.close();
            inputStream.close();
            return stringBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
```

## Broadcast-сообщение

При вызове метода visit и создании фото запускается фоновый процесс передачи фото и получения отчетов, который по завершении формирует broadcast сообщение **com.intrtl.app.BROADCAST_VISIT_COMPLETED**.

### Содержимое broadcast-сообщения

Поле  | Описание
------------- | -------------
visit_id | ИД визита
internal_visit_id | внутренний ИД визита
user_id | ИД пользователя
store_id | ИД торговой точки
total_photos | общее количество фото (не входят фото плохого качества)
completed_photos | количество обработанных фото
result | URI (строка) файла с [отчетом](#пример-отчета-поле-result-в-broadcast-и-getdata-в-onactivityresult)

### Пример обработки broadcast-сообщения

```java
BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            try {
                String reportString = readFromUri(new Uri().parse(extras.getString("result")))
                JSONObject reportJson = new JSONObject(reportString);                
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
};

this.registerReceiver(broadcastReceiver, new IntentFilter("com.intrtl.app.BROADCAST_VISIT_COMPLETED"));
```

## Пример отчета (поле result в broadcast и getData() в onActivityResult)

[Пример отчета](https://github.com/intrtl/AiletLibraryExamples/blob/master/Android/IrIntentExample/report_exaple.json)

## Пример взаимодействия

- Вызовать приложение Ailet с методом visit 
- Выполнить визит с несколькими фото
- Выйти из приложения Ailet
- Проверить результат, если RESULT_INPROGRESS, то необходимо ожидать бродкаст сообщение о готовности, если RESULT_OK, то отчет содержит готовые данные
- При получении бродкаста со статусом RESULT_OK обработать отчет, он содержит готовые данные, так же можно вызвать приложение Ailet с методом report или summaryReport

## Возможные проблемы при интеграции

### Особенности Android 11

Если в проекте используется targetSdkVersion 30, то может возникнуть проблема с вызовом приложение Ailet, для ее решения есть несколько сопособов:

- добавить queries в AndroidManifest (предпочтительный вариант)

    ```xml
    <queries>
        <package android:name="com.intrtl.app" />
    </queries>
    ```

- добавить queries в AndroidManifest 

    ```xml
    <queries>
        <intent>
            <action android:name="com.intrtl.app.ACTION_VISIT" />
        </intent>
        <intent>
            <action android:name="com.intrtl.app.ACTION_REPORT" />
        </intent>
        <intent>
            <action android:name="com.intrtl.app.ACTION_SUMMARY_REPORT" />
        </intent>
        <intent>
            <action android:name="com.intrtl.app.ACTION_SYNC" />
        </intent>
    </queries>
    ```

- добавить QUERY_ALL_PACKAGES в AndroidManifest 
  
    ```xml
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>
    ```

- понизить targetSdkVersion до версии 29
