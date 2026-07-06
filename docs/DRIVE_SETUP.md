# Configurar o backup no Google Drive

O FitTrack guarda backups na **pasta oculta do app** no Drive (`appDataFolder`) — os arquivos
não aparecem no seu Drive normal e só o próprio app consegue acessá-los.

Para o login Google funcionar, é preciso registrar o app num projeto do Google Cloud
(uma vez só, sem custo):

## Passo a passo

1. Acesse o [Google Cloud Console](https://console.cloud.google.com/) e crie um projeto
   (ex.: `fittrack`).
2. Em **APIs e serviços → Biblioteca**, procure **Google Drive API** e clique em **Ativar**.
3. Em **APIs e serviços → Tela de permissão OAuth**:
   - Tipo de usuário: **Externo**
   - Preencha nome do app e e-mail; em **Escopos**, adicione
     `https://www.googleapis.com/auth/drive.appdata`
   - Em **Usuários de teste**, adicione o(s) e-mail(s) que vão usar o app
     (enquanto o app estiver em modo "Teste", só esses e-mails conseguem logar).
4. Em **APIs e serviços → Credenciais → Criar credenciais → ID do cliente OAuth**:
   - Tipo: **Android**
   - Nome do pacote: `com.fittrack.app`
   - SHA-1: a impressão digital do certificado de assinatura.

## Como obter o SHA-1

- **Build de release (APK do GitHub Releases):** use o keystore de release:

  ```
  keytool -list -v -keystore fittrack-release.keystore -alias <alias>
  ```

- **Build de debug (Android Studio):**

  ```
  keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android
  ```

Registre **os dois** SHA-1 (crie dois clients OAuth Android) se quiser que o login
funcione tanto no build de debug quanto no de release.

## Observações

- Não é preciso `google-services.json` — o Google Sign-In casa o app pelo pacote + SHA-1.
- Se o login falhar com erro `12500`/`DEVELOPER_ERROR`, o SHA-1 registrado não bate com o
  certificado que assinou o APK instalado.
- O app mantém os **5 backups mais recentes** no Drive e apaga os antigos automaticamente.
