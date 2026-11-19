// The checklist of an `html` step: the same tests the website runs, see `apps/frontend/src/utils/html-tests.js` in `alt`.
//
// The server doesn't check an `html` step, it takes the score the client sends. So a check passes in the IDE exactly when
// it passes on the website. The libraries the tests need are in `lib/`, see `lib/README.md`.
window.hsHtmlTests = (() => {
  function checkRegexp(regExp, content) {
    const parsed = regExp.match(/^\/(.*)\/([a-z]{0,20})$/);
    if (!parsed) return null;

    const flags = `${parsed[2] || ''}g`
      .split('')
      .filter((flag) => 'gimsuy'.includes(flag))
      .filter((flag, index, all) => all.indexOf(flag) === index)
      .join('');
    return new RegExp(parsed[1], flags).test(content);
  }

  function htmlHint() {
    const module = window.HTMLHint;
    return module && (module.HTMLHint || module);
  }

  function isValidHtml(html) {
    const hint = htmlHint();
    if (!hint || !hint.verify) return false;

    const messages = hint.verify(html, {
      'tagname-lowercase': true,
      'attr-lowercase': true,
      'attr-value-double-quotes': false,
      'doctype-first': false,
      'tag-pair': true,
      'spec-char-escape': true,
      'id-unique': true,
      'src-not-empty': true,
      'attr-no-duplication': true,
    });
    return !messages.some((message) => message.type === 'error');
  }

  function isValidCss(css) {
    const validator = window.csstreeValidator;
    if (!validator || !validator.validate) return false;
    return validator.validate(css).length === 0;
  }

  const tests = {
    hasElement(element, {childSel}) {
      if (!element) return false;
      return childSel ? !!element.querySelector(childSel) : true;
    },

    checkClass(element, {className}) {
      if (element.classList) return element.classList.contains(className);
      return new RegExp(`(^| )${className}( |$)`, 'gi').test(element.className);
    },

    checkAttribute(element, {attr, attrValue}) {
      return element.getAttribute(attr) === attrValue;
    },

    // Comparing computed values makes aliases, shorthands and relative values work: `color: red`, `border: 1px solid`,
    // `width: 50%`. The expected values are the ones the element gets with the expected rule applied.
    checkCssStyle(element, {ruleName, value}) {
      if (!(ruleName in element.style)) return false;

      const shorthands = window.cssShorthandProps;
      const longhands = shorthands ? [shorthands.expand(ruleName, true)].flat(Infinity) : [ruleName];
      const computedStyle = element.ownerDocument.defaultView.getComputedStyle(element);
      const actualValues = longhands.map((property) => computedStyle.getPropertyValue(property));

      const originalStyle = element.style.cssText;
      element.style.setProperty(ruleName, value, 'important');
      const expectedValues = longhands.map((property) => computedStyle.getPropertyValue(property));
      element.style.cssText = originalStyle;

      return actualValues.every((actual, index) => actual === expectedValues[index]);
    },

    checkContent(element, {contentExpression}) {
      const content = element.innerHTML;
      const result = checkRegexp(contentExpression, content);
      return result !== null ? result : content === contentExpression;
    },

    checkSource(element, {sourceType, sourceExpression}, sources) {
      const source = sources[sourceType];
      if (!source) return false;
      return !!checkRegexp(sourceExpression, source);
    },

    checkValidityOfSources(element, {sourceType}, sources) {
      if (!sourceType) return false;
      const checksHtml = sourceType === 'html + css' || sourceType === 'html';
      const checksCss = sourceType === 'html + css' || sourceType === 'css';
      return (!checksHtml || isValidHtml(sources.html)) && (!checksCss || isValidCss(sources.css));
    },
  };

  function runCheck(check, document, sources) {
    const target = document.querySelector(check.selector || 'body');
    if (!target) return false;

    return (check.tests || []).every(({type, data}) => {
      const test = tests[type];
      if (!test) return false;
      try {
        return !!test(target, data || {}, sources);
      }
      catch (e) {
        console.warn(`The ${type} test failed with an error`, e);
        return false;
      }
    });
  }

  // Like on the website, the page is rendered from the HTML with the CSS put into its head, in place of a link to
  // `style.css` if there is one, so that the tests see the styles applied.
  function renderPage(frame, sources) {
    const page = new DOMParser().parseFromString(sources.html.replace(/href="/g, 'target="_blank" href="'), 'text/html');
    const head = page.head;
    const firstHeadNode = head.firstChild;

    const baseStyle = page.createElement('style');
    baseStyle.textContent = 'body { font-family: system-ui, -apple-system, Segoe UI, Roboto, Helvetica Neue, Arial, sans-serif; margin: 0; }';
    head.insertBefore(baseStyle, firstHeadNode);

    const userStyle = page.createElement('style');
    userStyle.textContent = sources.css || '';
    const styleLink = head.querySelector('link[href="style.css"]') || head.querySelector('link[href="./style.css"]');
    head.insertBefore(userStyle, styleLink || firstHeadNode);
    if (styleLink) head.removeChild(styleLink);

    const document = frame.contentWindow.document;
    document.replaceChild(document.importNode(page.documentElement, true), document.documentElement);
    return document;
  }

  /**
   * Renders the page in the [frame] and runs the [checklist] on it.
   *
   * @return the result of every check and the score the server expects: 1 if all of them pass, 0 otherwise
   */
  function run(frame, checklist, sources) {
    const document = renderPage(frame, sources);
    const results = (checklist || []).map((check) => ({name: check.name, isSuccess: runCheck(check, document, sources)}));
    return {results, score: results.every((result) => result.isSuccess) ? 1 : 0};
  }

  return {run};
})();
