; ============================================================================
; Kaiteyo — Branding.iss
; Premium copy and typography for the modern wizard.
; ============================================================================

; ---- Friendly progress copy (real progress, nice words) ----
[Messages]
SetupAppTitle=Kaiteyo Installer
SetupWindowTitle=Kaiteyo {#MyAppVersion} Installer
BeveledLabel=書いてよ — Learn Japanese, beautifully
SelectDirDesc3=Choose where Kaiteyo will live on this computer
SelectDirLabel3=Where should Kaiteyo be installed?
SelectDirBrowseLabel=Setup will install Kaiteyo into the following folder.
ReadyLabel1=Kaiteyo is ready to install on your computer.
ReadyLabel2a=Click Install to begin, or review the options below first.
ReadyLabel2b=Changes to your current installation will be applied now.
DiskSpaceMBLabel=Kaiteyo requires at least [mb] MB of free disk space.
InstallingLabel=Installing Kaiteyo — this only takes a moment…
FinishedHeadingLabel=Installation Complete
FinishedLabel=Kaiteyo has been installed and is ready to study with. 頑張って！
FinishedLabelNoIcons=Kaiteyo has been installed and is ready to study with. 頑張って！
WelcomeLabel1=Welcome to Kaiteyo
WelcomeLabel2=Your intelligent kanji learning companion.%n%nThe installer will guide you through a few quick choices. You can change everything later inside the app.
SelectTasksLabel2=Choose the extras you'd like — shortcuts, file associations and the free dictionary starter pack.
ClickNext=Click Next to continue, or Cancel to exit.

; ---- Typography: modern system fonts on every wizard surface ----
[Code]
procedure StyleWizardFonts();
begin
  WizardForm.Font.Name := 'Segoe UI';
  WizardForm.WelcomeLabel1.Font.Size := 20;
  WizardForm.WelcomeLabel1.Font.Style := WizardForm.WelcomeLabel1.Font.Style + [fsBold];
  WizardForm.WelcomeLabel2.Font.Size := 9;
end;

procedure InitializeWizard();
begin
  StyleWizardFonts();
end;
