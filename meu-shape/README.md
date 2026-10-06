# Meu Shape

App Android pessoal, 100% offline, para rotina de dieta, treino, sono e progresso
na escala de trabalho 12x36. Sem login, sem servidor, sem internet: tudo fica no celular (Room/SQLite).

## Etapas
1. ✅ Escala 12x36, calendário, tela Hoje com notificações, água e refeição livre
2. ✅ Compras e marmitas
3. ✅ Treinos
4. Sono e passos
5. Progresso, fotos, backup, sequência, widget e resumo de domingo

## Compilação
O workflow `.github/workflows/meu-shape.yml` gera o APK de debug a cada push que mexe nesta pasta
e publica numa Release `meushape-v1.0.N`. Assinado sempre com `keystore/meu-shape-debug.jks`.

Local: `cd meu-shape && ./gradlew assembleDebug testDebugUnitTest`
