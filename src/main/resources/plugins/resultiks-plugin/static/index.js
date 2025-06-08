'use strict';

/**
 * Загружает данные resultiks из файла resultiks.json с кэшированием.
 * @returns {Promise<Array>} Промис с массивом тестов.
 */
function fetchResultiks() {
    const path = location.pathname.endsWith('/') ? location.pathname : '/';
    if (typeof allure.resultiks === 'undefined') {
        return fetch(path + 'data/resultiks.json')
            .then(response => response.json())
            .then(tests => allure.resultiks = tests);
    } else {
        return Promise.resolve(allure.resultiks);
    }
}

/**
 * Возвращает массив DOM-элементов тестов (a.node).
 * @returns {Array<Element>} Массив ссылок на тесты.
 */
function testNodes() {
    return [...document.body.querySelectorAll('a.node')];
}

/**
 * Проверяет, отмечен ли тест специальной меткой.
 * @param {Element} aNode - DOM-элемент теста.
 * @returns {boolean} true, если тест уже отмечен.
 */
function isMarked(aNode) {
    return !!aNode.querySelector('.y-label_status_unknown');
}

/**
 * Проверяет, есть ли на странице тесты.
 * @returns {boolean} true, если есть хотя бы один тест.
 */
function pageHaveTests() {
    return !!document.body.querySelector('a.node');
}

const TOOLTIP = 'Resultiks count';

/**
 * Добавляет к тесту метку с количеством resultiks.
 * @param {Element} testNode - DOM-элемент теста.
 * @param {number} resultiksCount - Количество resultiks для теста.
 */
function mark(testNode, resultiksCount) {
    if (!resultiksCount)
        return;
    if (isMarked(testNode))
        return;
    let mark = document.querySelector('.y-label_status_unknown')?.parentNode?.cloneNode(true);
    if (!mark)
        return;
    let span = mark.querySelector('span');
    span.textContent = resultiksCount;
    span.setAttribute('data-tooltip', TOOLTIP);
    testNode.insertBefore(mark, testNode.querySelector('.node__stats'));
}

/**
 * Отмечает все тесты на странице, у которых есть resultiks.
 * @returns {Promise<void>} Промис, который резолвится после разметки.
 */
function markTests() {
    return fetchResultiks().then(tests => {
        tests.forEach(test => {
            let testNode = document.querySelector(`div[data-uid='${test.uid}']`);
            if (testNode && !isMarked(testNode)) {
                mark(testNode, test.resultiksCount);
            }
        });
    });
}

/**
 * Считает сумму resultiks в группе и отмечает группу.
 */
function markGroups() {
    document.querySelectorAll('span.node__stats').forEach(groupMark => {
        let group = groupMark.parentElement;
        let tests = group.parentElement.querySelectorAll(`span[data-tooltip='${TOOLTIP}']`);
        let resultiksCount = [...tests].reduce((sum, test) => sum + Number(test.textContent), 0);
        mark(group, resultiksCount);
    });
}

/**
 * Проверяет, нужно ли удалить или переместить виджет resultiks.
 * @type {Object}
 */
let widget0 = {
    /**
     * Возвращает корневой элемент сетки виджетов.
     * @returns {Element|null}
     */
    root() {
        return document.querySelector('.widgets-grid');
    },
    /**
     * Возвращает DOM-элемент виджета resultiks.
     * @returns {Element|null}
     */
    element() {
        return this.root()?.querySelector('.widget[data-id="resultiks"]');
    },
    /**
     * Проверяет, существует ли виджет resultiks.
     * @returns {boolean}
     */
    exists() {
        return !!this.element();
    },
    /**
     * Проверяет, нужно ли удалить виджет (если нет тестов с resultiksCount > 0).
     * @returns {Promise<boolean>} true, если нужно удалить.
     */
    checkIfMustBeDeleted() {
        // Проверяем, нужно ли удалять виджет (например, если нет тестов с resultiksCount > 0)
        return fetchResultiks().then(tests => {
            return !tests.length || tests.every(test => test.resultiksCount === 0); // Удаляем, если все resultiksCount === 0
        });
    },
    /**
     * Проверяет, нужно ли переместить виджет выше (на второе место).
     * @returns {boolean}
     */
    needsToMoveUp() { // ожидается порядок виджетов: summary, resultiks, suites, ...
        if (!this.exists()) return false;
        const widgets = [...this.root().querySelectorAll('.widget')];
        return widgets.indexOf(this.element()) !== 1; // Виджет должен быть вторым
    },
    /**
     * Перемещает или удаляет виджет resultiks, если это необходимо.
     * @returns {Promise<void>}
     */
    async moveUpOrDelete() {
        if (this.isAlreadyMoved) return;
        try {
            // Проверяем, нужно ли удалить виджет
            const mustBeDeleted = await this.checkIfMustBeDeleted();
            if (mustBeDeleted && this.exists()) {
                this.element().remove(); // Удаляем виджет
                return;
            }

            // Если виджет не нужно удалять, проверяем, нужно ли переместить его выше
            if (this.needsToMoveUp()) {
                const reference = this.root().querySelectorAll('.widget')[1]; // Второй виджет
                reference.parentElement.insertBefore(this.element(), reference);
                this.isAlreadyMoved = true;
            }
        } catch (error) {
            console.error('Ошибка при выполнении moveUpOrDelete:', error);
        }
    }
};

/**
 * Объект для разметки тестов resultiks на странице.
 * @type {Object}
 */
let resultikMarks0 = {
    /**
     * Устанавливает метки на тесты, если это необходимо.
     */
    setUp() {
        if (typeof this.hasResultiks === 'undefined') // пока не известно, есть ли тесты с resultiksCount > 0
            fetchResultiks().then(tests => this.hasResultiks = tests.some(t => t.resultiksCount));

        if (!this.hasResultiks) // в файле resultiks.json нет тестов с resultiksCount > 0
            return;

        let alreadyMarked = testNodes().some(isMarked);

        if (alreadyMarked)
            return;

        if (pageHaveTests())
            markTests().then(markGroups);
    }
};

// Наблюдатель за изменениями в DOM
const observer = new MutationObserver(() => {
    // на главной странице нужно установить виджет под summary
    widget0.moveUpOrDelete();
    // на остальных страницах нужно добавить маркеры на тесты
    resultikMarks0.setUp();
});

// Начинаем наблюдение за body (или другим контейнером, который обновляется)
observer.observe(document.body, { childList: true, subtree: true });







// === Resultiks Widget (Allure API way, Backbone.Model) ===
(function() {
    var ResultiksModel = Backbone.Model.extend({
        fetch: function() {
            var self = this;
            return fetchResultiks().then(tests => {
                var total = tests.reduce((sum, t) => sum + (t.resultiksCount || 0), 0);
                self.set({ total });
                return self;
            });
        }
    });

    var resultiksTemplate = function(data) {
        return `<div class="widget__flex-line">
                    <div class="widget__column">
                        <div style="height: 50%;">
                            <h2 class="widget__title">RESULTIKS</h2>
                        </div> <!-- сдвиг текста вниз -->
                        <div style="text-align: center;">
                            <div style="line-height: 1.5em;" class="splash__subtitle">
                                Требуется анализ второстепенн${ data.total === 1 ? 'ой проверки' : 'ых проверок' }
                            </div>
                        </div>
                    </div>
                    <div class="widget__column summary-widget__chart">
                        <div style="padding-top: 45%;"> <!-- чуть меньше диаграммы summary -->
                            <svg id="triangle" class="chart__svg" width="60" height="60" viewBox="0 0 200 200" xmlns="http://www.w3.org/2000/svg">
                                <path d="M100 185 L25 185 Q15 180 20 170 L95 20 Q100 15 105 20 L180 170 Q185 180 175 185 L100 185 Z"
                                    fill="none" stroke="#d35ebe" stroke-width="1.4em">
                                </path>
                                <text x="100" y="100" dy="1em">${data.total}</text>
                            </svg>
                        </div>
                    </div>
                </div>`
    };

    class ResultiksWidgetView extends allure.components.WidgetStatusView {
        template = resultiksTemplate;
        serializeData() {
            return { total: this.model.get('total') };
        }
        // Переопределяем метод render, чтобы скрыть виджет, если total === 0
        render() {
            if (this.model.get('total') === 0) {
                this.$el.empty(); // Очищаем содержимое виджета
                widget0.element().remove();
                return this;
            }
            return super.render();
        }
    }

    allure.api.addWidget('widgets', 'resultiks', ResultiksWidgetView, ResultiksModel);
})();
