const form = document.querySelector('#search-form');
const input = document.querySelector('#query');
const submit = document.querySelector('#submit');
const status = document.querySelector('#status');
const results = document.querySelector('#results');
const section = document.querySelector('.results-section');
const suggestions = [...document.querySelectorAll('[data-query]')];

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (submit.disabled) return;
  const query = input.value.trim();
  results.replaceChildren();
  status.className = '';
  if (!query) {
    status.textContent = '請輸入搜尋關鍵字。';
    input.focus();
    return;
  }
  submit.disabled = true;
  suggestions.forEach(button => { button.disabled = true; });
  section.setAttribute('aria-busy', 'true');
  status.textContent = `正在搜尋「${query}」，請稍候…`;
  try {
    const response = await fetch(`/api/search?${new URLSearchParams({ q: query })}`, {
      signal: AbortSignal.timeout(30000)
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.message || '搜尋失敗，請稍後再試。');
    if (!Array.isArray(data.results)) throw new Error('搜尋結果格式異常，請稍後再試。');
    const fragment = document.createDocumentFragment();
    for (const item of data.results) {
      const url = new URL(item.url);
      if (!['https:', 'http:'].includes(url.protocol)) continue;
      const article = document.createElement('article');
      article.className = 'result';
      const source = document.createElement('p');
      source.className = 'source';
      source.textContent = item.url;
      const title = document.createElement('h2');
      const link = document.createElement('a');
      link.href = url.href;
      link.target = '_blank';
      link.rel = 'noopener noreferrer';
      link.textContent = item.title;
      title.append(link);
      article.append(title, source);
      fragment.append(article);
    }
    const count = fragment.childElementCount;
    results.append(fragment);
    status.textContent = count ? `「${data.query}」的搜尋結果 · 共 ${count} 筆` : `沒有找到「${data.query}」的結果，試試其他關鍵字。`;
  } catch (error) {
    status.className = 'error';
    status.textContent = error.name === 'TimeoutError' ? '搜尋等待逾時，請稍後再試。'
      : error instanceof TypeError ? '無法連線至搜尋服務，請確認網路後再試。' : error.message;
  } finally {
    submit.disabled = false;
    suggestions.forEach(button => { button.disabled = false; });
    section.setAttribute('aria-busy', 'false');
  }
});

suggestions.forEach(button => button.addEventListener('click', () => {
  input.value = button.dataset.query;
  form.requestSubmit();
}));
