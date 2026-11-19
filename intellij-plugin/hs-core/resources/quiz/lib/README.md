# Libraries of the html step checklist

The checklist of an `html` step is checked on the client, by the same tests as on the website
(`apps/frontend/src/utils/html-tests.js` in `alt`). The tests need these libraries, in the versions the website uses:

| File | Package | License |
|---|---|---|
| `htmlhint.min.js` | [htmlhint](https://www.npmjs.com/package/htmlhint) 1.9.2, `dist/htmlhint.min.js` | MIT, `htmlhint.LICENSE.md` |
| `csstree-validator.min.js` | [csstree-validator](https://www.npmjs.com/package/csstree-validator) 2.0.1, `dist/csstree-validator.js` | MIT, `csstree-validator.LICENSE` |
| `css-shorthand-properties.js` | [css-shorthand-properties](https://www.npmjs.com/package/css-shorthand-properties) 1.1.2, `index.js` | MIT, `css-shorthand-properties.LICENSE` |

The files are copied from the npm packages unchanged. To update one, take the same file from a newer package version.
