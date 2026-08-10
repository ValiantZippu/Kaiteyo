; ============================================================================
; Kaiteyo — Uninstall.iss
; A polite uninstaller: explains exactly what will be removed and never
; deletes study data without an explicit, labelled choice.
; ============================================================================

[Code]
var
  UninstallDataPage: TInputOptionWizardPage;
  UninstallRemoveCacheCheck: TNewCheckBox;



{ ------------------------------------------------------------------
  Build the uninstaller's "What will be removed?" page.
  Default: keep study data. "Remove everything" is opt-in.
------------------------------------------------------------------- }
procedure InitializeUninstall;
var
  DataDir: string;
begin
  UninstallDataPage := CreateInputOptionPage(
    wpWelcome, 'Remove Kaiteyo?', 'Tell us what to delete',
    'Kaiteyo will remove the application files, shortcuts and registry entries.' + #13#10#13#10 +
    'Your study progress, decks and settings are stored separately in:' + #13#10 +
    '  ' + ExpandConstant('{localappdata}\' + DataDirName) + #13#10#13#10 +
    'Would you like to keep them?',
    True, False);

  UninstallDataPage.Add('Keep my study data and settings (recommended)');
  UninstallDataPage.Add('Remove my study data and settings as well');
  UninstallDataPage.Values[0] := True;

  UninstallRemoveCacheCheck := TNewCheckBox.Create(UninstallDataPage);
  UninstallRemoveCacheCheck.Parent := UninstallDataPage.Surface;
  UninstallRemoveCacheCheck.Left := 0;
  UninstallRemoveCacheCheck.Top := 116;
  UninstallRemoveCacheCheck.Width := UninstallDataPage.SurfaceWidth;
  UninstallRemoveCacheCheck.Caption := 'Also delete cache and temporary files';
  UninstallRemoveCacheCheck.Checked := True;
end;

{ ------------------------------------------------------------------
  Apply the user's choice at the point of no return.
  Study data is only touched when explicitly requested.
------------------------------------------------------------------- }
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  DataDir: string;
begin
  if CurUninstallStep = usUninstall then begin
    DataDir := ExpandConstant('{localappdata}\' + DataDirName);

    if UninstallRemoveCacheCheck.Checked then begin
      { Cache/temp only — safe to delete without confirmation. }
      DelTree(DataDir + '\cache', True, True, True);
      DelTree(DataDir + '\logs', True, True, True);
      DelTree(DataDir + '\tmp', True, True, True);
    end;

    if not UninstallDataPage.Values[0] then begin
      { Explicit opt-in to full removal. Still confirm once more. }
      if MsgBox(
           'You chose to REMOVE your study data.' + #13#10#13#10 +
           'This deletes all decks, progress, reviews and settings in:' + #13#10 +
           '  ' + DataDir + #13#10#13#10 +
           'This cannot be undone. Continue?',
           mbConfirmation, MB_YESNO) = IDYES then
        DelTree(DataDir, True, True, True)
      else
        UninstallDataPage.Values[0] := True; { user backed out — keep data }
    end;
  end;
end;
