# Logo Prompt for ChatGPT

Use the following prompt (in English or Russian as you prefer) to generate a
logo and adaptive launcher icon for **WakeSync**.

---

## English version

You are designing the app icon and marketing logo for **WakeSync**, an open-source,
feature-rich alarm clock for Android + Wear OS. Repository:
https://github.com/serejaishkin/alarm-sync-wear

**Context / brand:**
- Product name: **WakeSync** — a play on "wake" and "sync" (it syncs alarms between a
  phone and a Wear OS watch). It is a full alarm clock: smart wake, 30+ dismiss
  challenges, iOS-style reliability, deep dark theme.
- Rebranded from "AlarmClockXtreme"; remove all traces of the old brand.
- The app's UI is a **dark blue Material 3** theme (deep navy background, cyan/blue
  accents). Current launcher icon is a **cream/white bell** (see screenshots in the
  repo, e.g. `assets/screenshots/alarm-list.png`).
- Tone: calm, trustworthy, slightly premium; no ads/no tracking privacy-minded app.

**Deliverables I need:**
1. **Adaptive launcher icon** for Android:
   - foreground + background layers (384×384 safe zone, 108×108dp grid),
   - visible result on a **dark** background (it must read well on dark launchers),
   - also render a **monochrome** version (single-color layer for themed icons,
     Android 13+).
   - legacy PNG fallbacks exported at: `mdpi 48`, `hdpi 72`, `xhdpi 96`,
     `xxhdpi 144`, `xxxhdpi 192` px.
2. **Marketing logo** (horizontal lockup, e.g. bell icon + wordmark "WakeSync"),
   on transparent + dark backgrounds, SVG and PNG.
3. A short color palette (hex values) you used, so I can match it in-app.

**Style hints:** minimalist flat vector bell (or sunrise-through-a-bell) in
cream/amber on deep navy (`#0B1220`-ish), cyan accent. No gradients except subtle
ones allowed; keep it crisp at 48px. No clipart, no realistic 3D, no skeuomorphism.

---

## Русская версия

Ты — дизайнер иконок. Разработай логотип и адаптивную иконку приложения для
**WakeSync** — бесплатного open-source будильника для Android + Wear OS.
Репозиторий: https://github.com/serejaishkin/alarm-sync-wear

**Контекст / бренд:**
- Название: **WakeSync** (от «wake» + «sync» — будильники синхронизируются между
  телефоном и часами Wear OS). Это полноценный будильник: умное пробуждение,
  30+ челленджей для отключения, тёмная тема.
- Приложение переименовано из «AlarmClockXtreme» — следов старого бренда быть не должно.
- Тема UI — **тёмно-синяя Material 3** (глубокий тёмно-синий фон, голубые/циановые
  акценты). Текущая иконка — **кремовый/белый колокольчик** (скриншоты в репо,
  например `assets/screenshots/alarm-list.png`).
- Тон: спокойный, надёжный, немного премиальный; приватность без рекламы и трекинга.

**Что нужно получить:**
1. **Адаптивная иконка** Android:
   - слои foreground + background (безопасная зона 384×384, сетка 108×108dp),
   - результат должен читаться на **тёмном** фоне,
   - отдельно — **monochrome**-версия (один слой для тематических иконок Android 13+),
   - legacy PNG: `mdpi 48`, `hdpi 72`, `xhdpi 96`, `xxhdpi 144`, `xxxhdpi 192` px.
2. **Маркетинговый логотип** (горизонтальная связка: колокольчик + слово «WakeSync»),
   на прозрачном и тёмном фоне, в SVG и PNG.
3. Короткая палитра (hex-значения), чтобы я повторил её в коде приложения.

**Пожелания по стилю:** минималистичный плоский векторный колокольчик (или луч
рассвета сквозь колокольчик) в кремово-янтарных тонах на глубоком тёмно-синем
(около `#0B1220`), циановый акцент. Никаких клипартов, 3D и скевоморфизма; аккуратно
при 48 px; допускаются только лёгкие градиенты.