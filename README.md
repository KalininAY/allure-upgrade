## Проект является десктопным приложением, предназначенным для добавления в штатную поставку Allure2 плагина resultiks
Описание плагина и его исходный код можно найти в [репозитории](https://github.com/KalininAY/allure2-plugins)  
Принцип добавления плагина в Allure2 описан в разделе [Добавление плагина в Allure2](#добавление-плагина-в-allure2).  
Для начала работы см [Использование](#использование).

### Использование
1. Скачать zip-архив Allure2, который нужно модифицировать:
    - `allure-commandline-x.x.x.zip` из корпоративного репозитория;
    - `allure-x.x.x.zip` [из официфльного репозитория](https://github.com/allure-framework/allure2/releases)
1. Скачать [allure-resultiks-upgrade-x.x.jar](https://github.com/KalininAY/allure-upgrade/releases)
1. Запустить `allure-resultiks-upgrade-x.x.jar`
    - для запуска постребуется java 8+
1. Если требуется внести изменения в файлы плагина:
    - либо использовать для обновления файлы из ресурсов `allure-resultiks-upgrade-x.x.jar` (источник по умолчанию), а затем в обновленном архиве внести изменения в файлы;
    - либо создать папку с исходниками файлов плагина (`allure-plugin.yaml`, `resultiks-plugin-j17.jar`, `index.js`, `styles.css`), взятыми из [репозитория](https://github.com/KalininAY/allure2-plugins/releases), и после запуска allure-resultiks-upgrade-x.x.jar выбрать созданную папку как источник плагина

### Добавление плагина в Allure2
1. отредактировать файл `config/allure.yml`:
    - добавить название каталога, содержащего плагин. Например,
      ```yaml
        - resultiks-plugin
      ```
1. создать указанный ранее каталог в plugins
2. добавить файл `allure-plugin.yml` с перечнем файлов плагина и сами файлы плагина (`*.jar`, `*.js`, `*.css`)
    - файл `*.jar` добавляется в папку плагина, рядом с `allure-plugin.yml`;
    - файлы `*.js` и `*.css` в подпапку `static`
  
В итоге должна получиться следующая структура файлов:
```
   bin/
      ...
   config/
      allure.yml        # изменено
      ...
   lib/
      ...
   plugins/
      ...
      resultiks-plugin/     # добавлено
         static/
            index.js
            styles.css
         allure-plugin.yml
         resultiks-plugin-j17.jar
```
