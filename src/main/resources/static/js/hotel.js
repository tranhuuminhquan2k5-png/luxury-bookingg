document.querySelectorAll('form[data-confirm]').forEach(form => form.addEventListener('submit', event => {
  if (!window.confirm(form.dataset.confirm)) event.preventDefault();
}));
document.querySelectorAll('img[data-fallback]').forEach(img => img.addEventListener('error', () => {
  if (!img.dataset.failed) { img.dataset.failed = 'true'; img.src = img.dataset.fallback; }
}));
document.querySelectorAll('[data-print]').forEach(button => button.addEventListener('click', () => window.print()));
document.querySelectorAll('[data-back]').forEach(button => button.addEventListener('click', () => history.back()));
document.querySelectorAll('[data-table-search]').forEach(input => input.addEventListener('input', () => {
  const term = input.value.toLocaleLowerCase('vi');
  document.querySelectorAll('[data-search-row]').forEach(row => row.hidden = !row.textContent.toLocaleLowerCase('vi').includes(term));
}));
