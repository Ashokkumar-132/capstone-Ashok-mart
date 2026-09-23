document.addEventListener('DOMContentLoaded', function () {
    const registerForm = document.querySelector('[data-register-form]');
    if (!registerForm) {
        return;
    }

    registerForm.addEventListener('submit', function (event) {
        const password = registerForm.querySelector('#password');
        const confirmation = registerForm.querySelector('#confirmPassword');
        if (password && confirmation && password.value !== confirmation.value) {
            event.preventDefault();
            confirmation.setCustomValidity('Passwords do not match.');
            confirmation.reportValidity();
        } else if (confirmation) {
            confirmation.setCustomValidity('');
        }
    });
});
