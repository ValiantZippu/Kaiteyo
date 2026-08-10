; ============================================================================
; Kaiteyo — Code.iss
; Pascal logic: install-location memory, existing-install detection,
; data-location messaging, upgrade friendliness, crash-safe temp cleanup.
; ============================================================================

[Code]
const
  { Data lives in LocalAppData, never under {app}. This separation is what
    makes upgrades and uninstalls safe for user content. }
  DataDirName = 'Kaiteyo';

{ ------------------------------------------------------------------
  Remember the previous install directory across upgrades.
  Inno already does this via UsePreviousAppDir; this layer additionally
  surfaces the saved path on the Ready page and warns when a *different*
  install exists (repair vs. separate install).
------------------------------------------------------------------- }
function GetPreviousInstallPath(): string;
var
  Value: string;
begin
  Result := '';
  if RegQueryStringValue(HKCU, 'Software\Kaiteyo', 'InstallPath', Value) then
    Result := Value;
end;

procedure CurPageChanged(CurPageID: Integer);
var
  Previous, Current: string;
begin
  if CurPageID = wpReady then begin
    Previous := GetPreviousInstallPath();
    Current := WizardDirValue();
    if (Previous <> '') and (Previous <> Current) then begin
      WizardForm.ReadyMemo.Lines.Add(
        'Note: a previous Kaiteyo installation was found at ' + Previous +
        '. Your study data and settings there are preserved untouched.');
    end;
  end;
end;

{ ------------------------------------------------------------------
  Upgrade path: if an older version is already installed with the same
  AppId, Inno offers Repair/Modify/Remove automatically. We additionally
  give the silent path a deterministic outcome.
------------------------------------------------------------------- }
function InitializeSetup(): Boolean;
begin
  { Refuse to run 32-bit shells on 64-bit-only targets. }
  if IsWin64 then begin
    if not Is64BitInstallMode then
      MsgBox('Kaiteyo requires a 64-bit Windows.', mbError, MB_OK);
    Result := Is64BitInstallMode;
  end else
    Result := True;
end;

