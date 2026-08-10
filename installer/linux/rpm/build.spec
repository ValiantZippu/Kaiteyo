# ============================================================================
# Kaiteyo — RPM spec (Fedora / RHEL / openSUSE)
# Built by installer/linux/rpm/build.sh; kept in sync with the deb layout.
# ============================================================================
%global appname kaiteyo
%global appid   io.github.syt0r.kaiteyo

Name:           kaiteyo
Version:        %{_version}
Release:        1%{?dist}
Summary:        Japanese language learning app with writing exercises, flashcards and a built-in dictionary

License:        GPL-3.0-only
URL:            https://github.com/ValiantZippu/Kaiteyo
Source0:        kaiteyo-%{version}-image.tar.xz

BuildArch:      x86_64
Requires:       libX11, libXext, libXi, libXrender, libXtst, libGL, fontconfig

%description
Kaiteyo (書いてよ) is a premium Japanese language learning app. Learn kana and
kanji with writing exercises, review with FSRS spaced repetition, and look up
words in a built-in dictionary. Works fully offline.

%prep
%setup -q -c -n kaiteyo

%install
mkdir -p %{buildroot}%{_libdir}/kaiteyo
cp -r app/Kaiteyo/* %{buildroot}%{_libdir}/kaiteyo/

mkdir -p %{buildroot}%{_bindir}
cat > %{buildroot}%{_bindir}/kaiteyo <<'LAUNCHER'
#!/bin/sh
exec %{_libdir}/kaiteyo/bin/Kaiteyo "$@"
LAUNCHER
chmod +x %{buildroot}%{_bindir}/kaiteyo

# Desktop entry + AppStream
mkdir -p %{buildroot}%{_datadir}/applications %{buildroot}%{_datadir}/metainfo
install -m644 %{_sourcedir}/%{appid}.desktop %{buildroot}%{_datadir}/applications/
install -m644 %{_sourcedir}/%{appid}.metainfo.xml %{buildroot}%{_datadir}/metainfo/

# Icon theme
for size in 16 32 48 64 128 256 512; do
  mkdir -p %{buildroot}%{_datadir}/icons/hicolor/${size}x${size}/apps
  install -m644 %{_sourcedir}/kaiteyo-${size}.png \
    %{buildroot}%{_datadir}/icons/hicolor/${size}x${size}/apps/%{appid}.png
done
mkdir -p %{buildroot}%{_datadir}/icons/hicolor/scalable/apps
install -m644 %{_sourcedir}/kaiteyo.svg \
  %{buildroot}%{_datadir}/icons/hicolor/scalable/apps/%{appid}.svg

%post
touch --no-create %{_datadir}/icons/hicolor &>/dev/null || true
gtk-update-icon-cache %{_datadir}/icons/hicolor &>/dev/null || true
update-desktop-database %{_datadir}/applications &>/dev/null || true

%postun
gtk-update-icon-cache %{_datadir}/icons/hicolor &>/dev/null || true

%files
%{_bindir}/kaiteyo
%{_libdir}/kaiteyo
%{_datadir}/applications/%{appid}.desktop
%{_datadir}/metainfo/%{appid}.metainfo.xml
%{_datadir}/icons/hicolor/*/apps/%{appid}.png
%{_datadir}/icons/hicolor/scalable/apps/%{appid}.svg

%changelog
* Mon Aug 10 2026 syt0r - 2.2.1-1
- Premium branded installers; first-run onboarding; update architecture.
