module.exports = {
    parser: 'babel-eslint', // Default parser throws unexpected token error while the syntax is correct
    parserOptions: {
        ecmaVersion: 6,
        ecmaFeatures: {
            jsx: true,
            modules: true,
        },
    },
    env: {
        browser: true,
        es6: true,
        jest: true
    },
    extends: 'airbnb',
    rules: {
        'max-len': ['error', 120],
        'require-jsdoc': [
            'warn',
            {
                require: {
                    FunctionDeclaration: true,
                    MethodDefinition: true,
                    ClassDeclaration: true,
                },
            },
        ],
        'valid-jsdoc': [
            'warn',
            {
                requireReturn: false,
                requireReturnDescription: false,
            },
        ],
        indent: ['error', 4, { SwitchCase: 1 }],
        'import/no-extraneous-dependencies': [
            'off',
            {
                devDependencies: false,
                optionalDependencies: false,
                peerDependencies: false,
            },
        ],
        'import/no-unresolved': ['off'],
        'import/extensions': ['off'],
        'import/no-named-as-default': ['off'],
        'import/no-named-as-default-member': ['off'],
        'no-underscore-dangle': 0,
        'no-restricted-syntax': ['off'],
        'no-restricted-globals': ["off"],
        'no-plusplus': ['off'],
        "no-param-reassign": 0,
        'class-methods-use-this': ['off'],
        'arrow-body-style': 'off',
        'prefer-template': 'off',
        "react/prop-types": 0,
        'jsx-a11y/no-static-element-interactions': 'off',
        'jsx-a11y/no-noninteractive-element-interactions': 'off',
        'jsx-a11y/anchor-is-valid': 'off', // Due to using React-Router Link components
        'react/jsx-indent': ['error', 4],
        'react/jsx-indent-props': ['error', 4],
        'react/no-did-mount-set-state': ['off'], // Validity of this rule is questionable with react 16.3.0 onwards,
        // until this (https://github.com/yannickcr/eslint-plugin-react/issues/1754) issue resolved
        'no-mixed-operators': ['error'],
        'jsx-quotes': ['error', 'prefer-single'],
        'no-else-return': 'off',
        'no-unused-vars': ['error'],
    },
    plugins: ["react"],
};
