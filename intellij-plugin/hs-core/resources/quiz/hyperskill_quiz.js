// The form of a quiz step, shown under the task description in the task tool window.
//
// The plugin knows nothing about the block types: it passes the attempt dataset to `render` and gets back a reply in the
// format of the `reply` field of a submission. The formats are the ones of the website, see `components/step/problems/*.vue`.
window.hsQuiz = (() => {
  const MAX_PARSONS_LEVEL = 10;

  let stepId = 0;
  let send = () => {};

  const post = (message) => send(JSON.stringify({...message, step: stepId}));

  function element(tag, className, html) {
    const result = document.createElement(tag);
    if (className) result.className = className;
    if (html !== undefined) result.innerHTML = html;
    return result;
  }

  function typeset(container) {
    if (window.MathJax && window.MathJax.Hub) {
      window.MathJax.Hub.Queue(['Typeset', window.MathJax.Hub, container]);
    }
  }

  function required(value) {
    if (!Array.isArray(value) || value.length === 0) throw new Error('The dataset is empty');
    return value;
  }

  function moveButton(symbol, title, isDisabled, action) {
    const button = element('button', 'hs-quiz-move');
    button.type = 'button';
    button.textContent = symbol;
    button.title = title;
    button.setAttribute('aria-label', title);
    button.disabled = isDisabled;
    button.addEventListener('click', action);
    return button;
  }

  function renderChoice(container, {dataset, reply, disabled}, emit) {
    const options = required(dataset.options);
    const isMultiple = dataset.is_multiple_choice;
    const selected = options.map((_, index) => reply?.choices?.[index] === true);
    const update = () => emit(selected.some(Boolean) ? {choices: selected} : null);

    options.forEach((option, index) => {
      const input = element('input');
      input.type = isMultiple ? 'checkbox' : 'radio';
      input.name = 'hs-quiz-choice';
      input.checked = selected[index];
      input.disabled = disabled;
      input.addEventListener('change', () => {
        if (!isMultiple) selected.fill(false);
        selected[index] = input.checked;
        update();
      });

      const label = element('label', 'hs-quiz-option');
      label.append(input, element('div', 'hs-quiz-option-text', option));
      container.append(label);
    });
    update();
  }

  function isOrdering(value, size) {
    return Array.isArray(value)
      && value.length === size
      && [...value].sort((a, b) => a - b).every((item, index) => item === index);
  }

  function renderOrderedList(container, {size, ordering, itemHtml, captionHtml, disabled}, emit) {
    const current = isOrdering(ordering, size) ? [...ordering] : [...Array(size).keys()];
    const list = element('div', 'hs-quiz-list');

    function move(position, target) {
      [current[position], current[target]] = [current[target], current[position]];
      draw();
      emit({ordering: current});
    }

    function draw() {
      list.replaceChildren(...current.map((item, position) => {
        const row = element('div', 'hs-quiz-row');
        if (captionHtml) row.append(element('div', 'hs-quiz-caption', captionHtml(position)));

        const card = element('div', 'hs-quiz-card');
        card.append(
          element('div', 'hs-quiz-card-text', itemHtml(item)),
          moveButton('↑', 'Move up', disabled || position === 0, () => move(position, position - 1)),
          moveButton('↓', 'Move down', disabled || position === size - 1, () => move(position, position + 1)),
        );
        row.append(card);
        return row;
      }));
      typeset(list);
    }

    container.append(list);
    draw();
    emit({ordering: current});
  }

  function renderMatching(container, {dataset, reply, disabled}, emit) {
    const pairs = required(dataset.pairs);
    renderOrderedList(container, {
      size: pairs.length,
      ordering: reply?.ordering,
      itemHtml: (item) => pairs[item].second,
      captionHtml: (position) => pairs[position].first,
      disabled,
    }, emit);
  }

  function renderSorting(container, {dataset, reply, disabled}, emit) {
    const options = required(dataset.options);
    renderOrderedList(container, {
      size: options.length,
      ordering: reply?.ordering,
      itemHtml: (item) => options[item],
      disabled,
    }, emit);
  }

  function renderParsons(container, {dataset, reply, disabled}, emit) {
    const lines = required(dataset.lines);
    const current = Array.isArray(reply?.lines) && reply.lines.length === lines.length
      ? reply.lines.map((line) => ({...line}))
      : lines.map((_, index) => ({line_number: index, level: 0}));
    const list = element('div', 'hs-quiz-list');

    function changed() {
      draw();
      emit({lines: current});
    }

    function move(position, target) {
      [current[position], current[target]] = [current[target], current[position]];
      changed();
    }

    function indent(line, delta) {
      line.level += delta;
      changed();
    }

    function draw() {
      list.replaceChildren(...current.map((line, position) => {
        const text = element('div', 'hs-quiz-card-text hs-quiz-code', lines[line.line_number]);
        text.style.paddingLeft = `${line.level * 2}em`;

        const card = element('div', 'hs-quiz-card');
        card.append(
          text,
          moveButton('←', 'Decrease indent', disabled || line.level === 0, () => indent(line, -1)),
          moveButton('→', 'Increase indent', disabled || line.level === MAX_PARSONS_LEVEL, () => indent(line, 1)),
          moveButton('↑', 'Move up', disabled || position === 0, () => move(position, position - 1)),
          moveButton('↓', 'Move down', disabled || position === current.length - 1, () => move(position, position + 1)),
        );
        const row = element('div', 'hs-quiz-row');
        row.append(card);
        return row;
      }));
    }

    container.append(list);
    draw();
    emit({lines: current});
  }

  function renderTable(container, {dataset, reply, disabled}, emit) {
    const rows = required(dataset.rows);
    const columns = required(dataset.columns);
    const isCheckbox = dataset.is_checkbox;
    const answers = Array.isArray(reply?.choices) && reply.choices.length === rows.length
      ? reply.choices
      : rows.map((row) => ({name_row: row, columns: columns.map((column) => ({name: column, answer: false}))}));
    const update = () => emit(answers.some((row) => row.columns.some((column) => column.answer)) ? {choices: answers} : null);

    const table = element('table', 'hs-quiz-table');
    const header = element('tr');
    header.append(element('th'), ...columns.map((column) => element('th', '', column)));
    table.append(header);

    rows.forEach((rowName, rowIndex) => {
      const tableRow = element('tr');
      tableRow.append(element('td', 'hs-quiz-table-row-name', rowName));
      columns.forEach((_, columnIndex) => {
        const input = element('input');
        input.type = isCheckbox ? 'checkbox' : 'radio';
        input.name = `hs-quiz-row-${rowIndex}`;
        input.checked = answers[rowIndex].columns[columnIndex].answer;
        input.disabled = disabled;
        input.addEventListener('change', () => {
          const rowColumns = answers[rowIndex].columns;
          if (isCheckbox) rowColumns[columnIndex].answer = input.checked;
          else rowColumns.forEach((column, index) => column.answer = index === columnIndex);
          update();
        });
        const cell = element('td');
        cell.append(input);
        tableRow.append(cell);
      });
      table.append(tableRow);
    });
    container.append(table);
    update();
  }

  function renderTextInput(container, {reply, disabled}, emit, field, createInput, extraFields = {}) {
    const input = createInput();
    input.className = 'hs-quiz-input';
    input.value = reply?.[field] ?? '';
    input.disabled = disabled;
    input.placeholder = 'Your answer';

    const update = () => emit(input.value.trim() === '' ? null : {...extraFields, [field]: input.value});
    input.addEventListener('input', update);
    container.append(input);
    update();
  }

  function renderNumber(container, state, emit) {
    renderTextInput(container, state, emit, 'number', () => {
      const input = element('input');
      input.type = 'number';
      input.step = 'any';
      return input;
    });
  }

  function renderString(container, state, emit) {
    renderTextInput(container, state, emit, 'text', () => element('textarea'), {files: []});
  }

  function renderMath(container, state, emit) {
    renderTextInput(container, state, emit, 'formula', () => element('textarea'), {files: []});
  }

  function renderFillBlanks(container, {dataset, reply, disabled}, emit) {
    const [text, ...blanks] = required(dataset.components);
    const isSelect = blanks.every((blank) => blank.type === 'select');
    const isInput = blanks.every((blank) => blank.type === 'input');
    if (text.type !== 'text' || blanks.length === 0 || !(isSelect || isInput)) {
      throw new Error('Unsupported configuration of blanks');
    }

    const values = Array.isArray(reply?.blanks) && reply.blanks.length === blanks.length
      ? [...reply.blanks]
      : blanks.map(() => '');
    const update = () => emit(values.some((value) => value !== '') ? {blanks: values} : null);

    let slots = 0;
    const content = element('div', 'hs-quiz-blanks', text.text.replace(/▭/g, () => `<span class="hs-quiz-blank" data-index="${slots++}"></span>`));
    if (slots !== blanks.length) throw new Error('The number of blanks differs from the number of inputs');

    content.querySelectorAll('.hs-quiz-blank').forEach((slot) => {
      const index = Number(slot.dataset.index);
      const control = isSelect ? createBlankSelect(blanks[index].options) : createBlankInput();
      control.value = values[index];
      control.disabled = disabled;
      control.addEventListener('input', () => {
        values[index] = control.value;
        if (isInput) control.size = Math.max(control.value.length, 4);
        update();
      });
      if (isInput) control.size = Math.max(control.value.length, 4);
      slot.append(control);
    });
    container.append(content);
    update();
  }

  function createBlankInput() {
    const input = element('input', 'hs-quiz-input hs-quiz-blank-control');
    input.type = 'text';
    return input;
  }

  function createBlankSelect(options) {
    const select = element('select', 'hs-quiz-input hs-quiz-blank-control');
    for (const value of ['', ...options]) {
      const option = element('option');
      option.value = value;
      // Options can contain markup, for example <code>, which a native select can't show
      option.textContent = element('div', '', value).textContent;
      select.append(option);
    }
    return select;
  }

  const renderers = {
    'choice': renderChoice,
    'fill-blanks': renderFillBlanks,
    'matching': renderMatching,
    'math': renderMath,
    'number': renderNumber,
    'parsons': renderParsons,
    'sorting': renderSorting,
    'string': renderString,
    'table': renderTable,
  };

  function renderFallback(container, message, link) {
    const box = element('div', 'hs-quiz-message');
    box.textContent = message;
    if (link) {
      const anchor = element('a');
      anchor.href = link;
      anchor.textContent = 'Solve it on Hyperskill';
      box.append(' ', anchor);
    }
    container.replaceChildren(box);
  }

  function render(state) {
    const container = document.getElementById('hs-quiz');
    container.replaceChildren();
    if (state.error) {
      renderFallback(container, state.error, state.stepLink);
      return;
    }

    // A reply is sent even for the initial state, as a sorting has a valid answer before the learner touches anything.
    // Nothing can be answered in a disabled form, so there is nothing to send.
    const emit = state.disabled ? () => {} : (reply) => post({event: 'reply', reply});
    try {
      renderers[state.blockName](container, state, emit);
    }
    catch (e) {
      console.warn(`Can't show a quiz of type ${state.blockName}`, e);
      renderFallback(container, 'This problem can\'t be shown here.', state.stepLink);
      return;
    }
    typeset(container);
  }

  function start(id, sender) {
    stepId = id;
    send = sender;
    post({event: 'ready'});
  }

  return {start, render};
})();
