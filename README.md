# Tempo Família

App Android pessoal (instalado por APK) para gerenciar o tempo no celular, limitar redes sociais
e bloquear de forma definitiva o acesso a pornografia — para sobrar mais tempo de qualidade com a família.

Nada sai do celular: o app **não tem permissão de internet**, não tem anúncios nem análises.

## Funcionalidades

- **Limite de apps** — limite diário em minutos por app (medido com `UsageStatsManager`).
  Ao estourar, tela de bloqueio em tela cheia e volta para a tela inicial. Zera à meia-noite.
- **Bloqueio de palavras** — serviço de acessibilidade lê o texto digitado em qualquer app e o
  conteúdo/URL dos navegadores e apps de busca. Termo proibido → Voltar + Início + tela de bloqueio.
  Lista padrão em português e inglês + domínios adultos. Comparação ignora acentos, maiúsculas,
  espaços e pontuação.
- **Horários da família** — faixas por dia da semana em que todos os apps limitados ficam bloqueados.
- **Tela inicial** — uso e tempo restante de cada app e o próximo horário da família.
- **Trava de segurança** — senha (hash PBKDF2 + salt) criada por uma pessoa de confiança, exigida
  para afrouxar regras; Device Admin; bloqueio das telas de Configurações que desativam o app
  (página do app, Acessibilidade, Administradores do dispositivo, DNS privado, desinstalador);
  serviço em primeiro plano reiniciado no boot e após atualização.

## Compilação

O GitHub Actions (`.github/workflows/build.yml`) compila a cada push e, na branch principal,
publica o APK em uma Release. O APK é assinado sempre com `keystore/tempo-familia.jks`, então as
atualizações instalam por cima. O `versionCode` é o número da execução do workflow.

Para compilar localmente: `./gradlew assembleRelease` (requer Android SDK).

## Estrutura

```
app/src/main/java/br/com/tempofamilia/
├── data/      Store (DataStore), modelos, senha, lista padrão de termos
├── service/   GuardService (primeiro plano), GuardAccessibilityService, Enforcer, Boot/Admin
├── ui/        BlockActivity, componentes, telas (Compose + Material 3)
└── util/      medição de uso, normalização de texto, horários, permissões
```

## Limitações conhecidas

- No **modo de segurança** do Android os apps de terceiros não rodam; não há como impedir isso.
- A palavra "sexo" sozinha não está na lista padrão (aparece em formulários, "Sexo: masculino");
  expressões como "vídeos de sexo" estão. Você pode adicioná-la na aba Palavras se quiser.
