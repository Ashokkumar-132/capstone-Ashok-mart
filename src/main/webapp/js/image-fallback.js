document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('img[data-image-fallback]').forEach((image) => {
        image.addEventListener('error', () => {
            const fallback = document.createElement('span');
            fallback.className = 'image-placeholder';
            fallback.setAttribute('role', 'img');
            fallback.setAttribute('aria-label', 'Product image unavailable');
            fallback.innerHTML = 'Ashok<span>Mart</span>';
            image.replaceWith(fallback);
        }, { once: true });
    });
});
