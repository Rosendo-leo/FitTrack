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

## Fase 2 — Treinos (CRUD)
- [ ] Lista "Meus treinos" e "Pré-definidos"
- [ ] Editor de treino (criar/editar/excluir template)
- [ ] Adicionar/reordenar exercícios (drag & drop)
- [ ] Seed dos presets: PPL, ABC/ABCD, 5x5 StrongLifts, Full Body

## Fase 3 — Sessão Ativa
- [ ] Iniciar sessão a partir de um template
- [ ] Input de carga + reps por série, marcar warmup/RPE
- [ ] Timer de descanso (por exercício ou global)
- [ ] Detecção automática de PR
- [ ] Finalizar/pausar treino + cálculo de volume total

## Fase 4 — Progresso & Cardio
- [ ] Registro de peso + medidas corporais
- [ ] Registro de cardio (corrida/bike/nado) com timer
- [ ] Gráficos com Vico (peso, IMC, volume por tipo)
- [ ] Dashboard completo (delta semanal, streak, gráfico rápido)

## Fase 5 — Histórico
- [ ] Calendário de treinos
- [ ] Detalhe por sessão
- [ ] Volume total por semana, PR por exercício
- [ ] Filtros e busca

## Fase 6 — Notificações (WorkManager)
- [ ] Lembrete de treino por dia da semana
- [ ] Lembrete diário de peso
- [ ] Notificação persistente do timer de descanso
- [ ] Celebração de PR

## Fase 7 — Widgets (Glance)
- [ ] Treino do Dia (2×2 / 4×2)
- [ ] Peso Rápido (2×1 / 2×2)
- [ ] Progresso Semanal (4×2)
- [ ] Série Atual (4×2, apenas durante sessão)

## Fase 8 — Backup & Sync
- [ ] Export/import manual JSON + ZIP (com versão de schema)
- [ ] Google Drive API (OAuth2, pasta oculta do app)
- [ ] Sync automático via WorkManager
- [ ] Restore com resolução de conflitos (substituir/mesclar/cancelar)

## Fase 9 — Auto-update via GitHub Releases
- [ ] `UpdateChecker` (Retrofit → GitHub API, semver)
- [ ] Dialog de permissão com release notes
- [ ] Download + verificação SHA256
- [ ] Instalação via PackageInstaller
- [ ] Notificação de update em background

## Fase 10 — Polimento & Release
- [ ] Configurações completas (unidades kg/lb e km/mi, horários)
- [ ] Ícone final, splash screen
- [ ] Testes (DAOs, repositórios, detecção de PR)
- [ ] Assinatura do APK + primeiro GitHub Release (v1.0)
