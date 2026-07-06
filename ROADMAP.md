# FitTrack — Roadmap de fases

Baseado na esquemática em `fittrack-schema.html`.

## ✅ Fase 1 — Fundação (concluída)
- [x] Projeto Gradle (Kotlin DSL + version catalog)
- [x] Jetpack Compose + Material 3 (tema escuro/claro com a paleta da esquemática)
- [x] Hilt (DI) configurado
- [x] Room: 6 entidades (`WorkoutTemplate`, `Exercise`, `WorkoutSession`, `SetRecord`, `BodyMetric`, `CardioSession`) + DAOs + FKs/índices
- [x] DataStore para preferências (tema, unidades, notificações)
- [x] Repositórios (`WorkoutRepository`, `MetricsRepository`, `UserPreferencesRepository`)
- [x] Navegação com bottom bar (5 destinos) + telas placeholder
- [x] Dashboard inicial conectado ao banco (peso atual + sessões recentes)

## ✅ Fase 2 — Treinos (CRUD) (concluída)
- [x] Lista "Meus treinos" e "Pré-definidos" (abas + FAB)
- [x] Editor de treino (criar/editar/excluir template, categoria/objetivo)
- [x] Adicionar/remover/reordenar exercícios (botões ↑/↓; drag & drop fica para o polimento)
- [x] Seed dos presets: PPL (3), ABC (3), ABCD (4), 5x5 StrongLifts (2), Full Body (1)
- [x] Copiar preset para "Meus treinos" (presets são somente leitura)

## ✅ Fase 3 — Sessão Ativa (concluída)
- [x] Iniciar sessão a partir de um template (▶ na lista; retoma se já houver ativa)
- [x] Input de carga + reps por série, marcar aquecimento (RPE fica no polimento)
- [x] Timer de descanso global ajustável (−/+15s, pular)
- [x] Detecção automática de PR (snackbar 🏆)
- [x] Finalizar (volume total) / descartar / pausar (sair e retomar pelo Dashboard)
- [x] Cronômetro da sessão + volume e séries em tempo real

## ✅ Fase 4 — Progresso & Cardio (concluída)
- [x] Registro de peso + medidas corporais (% gordura, cintura, braço, peito)
- [x] Registro de cardio (tipo, duração, distância, calorias, FC) — timer integrado fica na Fase 10
- [x] Gráfico de peso (componente Canvas próprio; migrar para Vico no polimento) + minutos por tipo de cardio
- [x] Dashboard completo: delta semanal, streak 🔥, marcadores Seg–Dom, gráfico de peso e atalho registrar peso
- Nota: gráfico de IMC depende da altura do usuário → junto com Ajustes na Fase 10

## ✅ Fase 5 — Histórico (concluída)
- [x] Calendário mensal com dias treinados destacados + navegação entre meses
- [x] Filtro por dia (toque no calendário) e busca por nome do treino
- [x] Gráfico de volume por semana (últimas 8 semanas)
- [x] Recordes por exercício 🏆 (top 10, via query agregada)
- [x] Detalhe por sessão: data, duração, volume e séries por exercício, com exclusão

## ✅ Fase 6 — Notificações (WorkManager) (concluída)
- [x] Lembrete de treino por dia da semana (chips Seg–Dom + horário, reagenda no boot do app)
- [x] Lembrete diário de peso com horário configurável
- [x] Notificação persistente do timer de descanso (silenciosa, com progresso) + vibração ao fim
- [x] Notificação de PR 🏆 com vibração
- [x] Canais de notificação, permissão POST_NOTIFICATIONS e tela de Ajustes (tema + lembretes)
- Nota: timer como ForegroundService (sobreviver ao processo) fica para o polimento

## ✅ Fase 7 — Widgets (Glance) (concluída)
- [x] Treino do Dia (2×2/4×2): treino mais recente + nº de exercícios + botão iniciar
- [x] Peso Rápido (2×1/2×2): último peso + delta da semana + botão registrar
- [x] Progresso Semanal (4×2): ✓/· Seg–Dom + streak 🔥
- [x] Sessão Atual (4×2): nome do treino, séries e volume; estado vazio quando sem sessão
- [x] Atualização dos widgets disparada pelos ViewModels após cada mutação relevante
- Nota: ações abrem o app (deep link direto para iniciar treino/registrar peso fica no polimento)

## ✅ Fase 8 — Backup & Sync (concluída)
- [x] Export/import manual JSON + ZIP (com versão de schema; aceita .zip ou .json puro)
- [x] Google Drive API (Google Sign-In + Drive REST v3, `appDataFolder`; setup em `docs/DRIVE_SETUP.md`)
- [x] Sync automático via WorkManager (diário, com rede; retenção dos 5 backups mais recentes)
- [x] Restore com resolução de conflitos (substituir/mesclar/cancelar; merge idempotente com dedup)
- Nota: requer OAuth client Android (pacote + SHA-1) no Google Cloud Console — ver docs/DRIVE_SETUP.md

## ✅ Fase 9 — Auto-update via GitHub Releases (concluída)
- [x] `UpdateChecker` (Retrofit → GitHub API, comparação semver com BuildConfig.VERSION_NAME)
- [x] Dialog de permissão com release notes, tamanho do APK e "Atualizar agora / Depois"
- [x] Download com progresso + verificação SHA256 do asset publicado
- [x] Instalação via instalador do sistema (FileProvider) + fluxo REQUEST_INSTALL_PACKAGES
- [x] CI/CD: workflows de build e release assinada no GitHub Actions
- Nota: notificação de update em background fica junto com o BOOT receiver no polimento

## Fase 10 — Polimento & Release (em andamento)
- [x] Configurações completas (unidades kg/lb e km/mi aplicadas em todas as telas, altura do usuário)
- [x] IMC no Progresso (peso mais recente + altura dos Ajustes, classificação OMS)
- [x] RPE por série na sessão ativa (campo opcional; exibido no histórico)
- [x] Splash screen (core-splashscreen)
- [x] Dashboard: treinos recentes com nome e data
- [x] Ícone final (halter com traços arredondados, adaptive icon)
- [x] Widgets respeitando a unidade de peso escolhida
- [x] Testes unitários (conversão de unidades, semver do auto-update, serialização do backup) + CI roda `testDebugUnitTest`
- [ ] Assinatura do APK + primeiro GitHub Release: versão já em 1.0.0 — basta `git push` e `git tag v1.0.0 && git push --tags` (workflow de release assina e publica)
- Nota: testes de DAO/repositório exigem instrumentação (emulador) — fora do CI atual
- Pendências de polimento anteriores: drag & drop de exercícios, timer de cardio,
  gráfico com Vico, timer de descanso como ForegroundService, deep links dos widgets,
  notificação de update em background + BOOT receiver
