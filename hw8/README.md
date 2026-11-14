# HW8 拾光搜尋

Spring Boot / Java 21 搜尋網頁，透過後端呼叫 Tavily Search API，顯示最多 10 筆網頁標題與完整網址。

## 啟動

1. 安裝 JDK 21，至 https://app.tavily.com 取得 API Key。
2. 在 PowerShell 切換至此專案目錄，設定環境變數並啟動：

```powershell
$env:TAVILY_API_KEY = '在自己的終端機填入真正金鑰'
.\mvnw.cmd spring-boot:run
```

3. 開啟 http://localhost:8080，輸入關鍵字或點選建議搜尋。

使用 IDE 時，可在 Run Configuration 的環境變數設定 `TAVILY_API_KEY`，再執行 `Hw8Application`。
專案不會自動載入 `.env`。沒有金鑰時仍能啟動網頁，但搜尋會顯示服務尚未設定的提示。
可設定 `TAVILY_ENABLED=false` 暫停搜尋功能。

## 架構

- `static/`：HTML、CSS、JavaScript。使用者送出搜尋後呼叫同來源 API。
- `SearchController`：`GET /api/search?q=關鍵字`，檢查空白與 200 字元限制。
- `TavilySearchService`：以 Bearer 金鑰呼叫 Tavily，設定連線及讀取逾時，轉換結果並處理服務錯誤。
- `SearchResult`：結果的標題、網址與摘要；以列表保留同名標題。

API 成功回傳 `{ "query": "關鍵字", "results": [...] }`，錯誤回傳 `{ "message": "提示文字" }`。
空結果為成功回應，前端顯示「沒有找到結果」。外部文字透過 `textContent` 顯示，連結僅接受 HTTP / HTTPS。
金鑰只留在後端，不傳給瀏覽器。

API 文件：https://docs.tavily.com/documentation/api-reference/endpoint/search

## 測試

```powershell
.\mvnw.cmd test
```

自動測試模擬 Tavily HTTP 回應，不消耗額度，也不需要真實金鑰。
繳交前請使用自己的金鑰實際搜尋中文與英文，確認連結可開啟並截圖。

## 繳交

- 擷取任意關鍵字的真實搜尋結果畫面。
- 提交公開 GitHub 程式碼連結及截圖至 Moodle，每組一份。
- 截止時間：2026/11/15（日）23:59。
- 不要提交金鑰；`application.properties` 僅保留環境變數引用。
- 上層課程儲存庫的 `.gitignore` 排除了整個 `student/`。若使用上層儲存庫提交，需先調整忽略規則，讓 `student/hw8/` 可被追蹤；提交前確認 `target/`、IDE 設定與本機金鑰仍被忽略。也可以將本專案放到獨立的公開儲存庫。
