var currTheme

(() => {
    'use strict'

    const storedTheme = localStorage.getItem('theme')

    const getPreferredTheme = () => {
        if (storedTheme) {
            return storedTheme
        }

        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
    }

    const setTheme = function (theme) {
        if (theme === 'auto' && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            document.documentElement.setAttribute('data-bs-theme', 'dark')
            currTheme = 'dark'
        } else {
            document.documentElement.setAttribute('data-bs-theme', theme)
            currTheme = theme
        }
    }

    setTheme(getPreferredTheme())

    window.onload = () => {
        if (currTheme == 'dark') {
            document.querySelectorAll(".btn-outline-dark")
                .forEach((obj) => {
                    obj.classList.remove('btn-outline-dark');
                    obj.classList.add("btn-outline-light");
                });
            document.querySelectorAll(".btn-dark.selected-page")
                .forEach((obj) => {
                    obj.classList.remove('btn-dark');
                    obj.classList.add("btn-light");
                });
            document.querySelectorAll(".btn-light.noselected-page")
                .forEach((obj) => {
                    obj.classList.remove('btn-light');
                    obj.classList.add("btn-dark");
                });
            document.querySelectorAll(".table-light")
                .forEach((obj) => {
                    obj.classList.remove('table-light');
                    obj.classList.add("table-dark");
                });
            document.querySelectorAll("button.btn-dark")
                .forEach((obj) => {
                    obj.classList.remove('btn-dark');
                    obj.classList.add("btn-light");
                });
        }
    }

})()