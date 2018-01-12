FTL Profile/SavedGame Editor
https://github.com/Vhati/ftl-profile-editor


About

  Also known as the "ComaToes Profile/SavedGame Editor", this is a 3rd-party
  tool to edit user files. It depends on resources from the game directory,
  but the game itself will not be modified.

  With this, you can unlock any or all ships and achievements in your profile,
  or tweak most aspects of saved games: crew, fires, breaches, stores, etc.


Status

  FTL 1.5.4-1.6.2 profiles are fully editable. (ae_prof.sav)
  FTL 1.01-1.03.3 profiles are fully editable. (prof.sav)

  FTL 1.5.4-1.6.2 saved games are partially editable.
  FTL 1.01-1.03.3 saved games are fully editable.


Requirements

  Java (1.6 or higher).
    http://www.java.com/en/download/

  FTL (1.01-1.03.3 or 1.5.4-1.6.2, Windows/OSX/Linux, Steam/GOG/Standalone).
    https://subsetgames.com/


Setup

  Extract the files from this archive anywhere.

  On the first run, you may be prompted to locate your
  FTL resources. Specifically "data.dat" in the "resources/"
  directory under your FTL install.

  On OSX, you can select "FTL.app", because the resources are inside it.

  In most cases, this should be located automatically.


Usage

  Quit FTL to before editing profiles.

  For saved games, you must NOT be actively playing a campaign.
    FTL 1.5.4+: The main menu is safe.
    FTL 1.01-1.03.3: "Save+Quit".

  Double-click FTLProfileEditor.exe.
  Switch to the appropriate tab: "Profile" or "Saved Game".
  Open a profile (ae_prof.sav or prof.sav) or saved game (continue.sav).
  Make any desired changes, and save.

  Continue playing FTL.


Troubleshooting

* If double-clicking FTLProfileEditor.command doesn't work on Linux...
    Several terminals have bugs executing scripts whose paths contain spaces.
    There's no universal way to create a relative *.desktop shortcut either.

    For now, you'll need to open a terminal yourself.
      Then drag FTLProfileEditor.command onto that window, and hit enter.

    These might help?
      https://askubuntu.com/questions/617687/shell-script-does-not-run-when-i-double-click
      https://bugs.launchpad.net/ubuntu/+source/lubuntu-default-settings/+bug/975152

* If you get "java.lang.UnsupportedClassVersionError" on startup...
    You need a newer version of Java.

* If you get "java.lang.NoClassDefFoundError: javax/xml/bind/JAXBException"...
    That should only occur if you try to run "java -jar ..." yourself.
    Java 9 made one of its libraries opt-in, requiring a special argument.

    java --add-modules=java.xml.bind -jar FTLProfileEditor.jar

    The launcher straddles Java releases by telling older ones to ignore it.

    java -XX:+IgnoreUnrecognizedVMOptions ...
