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
