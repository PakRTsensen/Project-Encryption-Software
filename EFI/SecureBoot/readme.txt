Secure Boot:
In order to allow VeraCrypt EFI bootloader to run when EFI Secure Boot is enabled, VeraCrypt EFI bootloader files are signed by custom key(DCS_sign) 
whose public part can be loaded into Secure Boot to allow verification of VeraCrypt EFI files.

to update Secure Boot configuration steps:
1. Run the tool dumpEfiVars (https://www.veracrypt.fr/downloads/tools/dumpEfiVars.exe) to dump the SecureBoot data.
2. Go through all folders created by dumpEfiVars (other than "77fa9abd-0359-4d32-bd60-28f4e78f784b" and "SigLists") and note the file names of the certificates created inside the folders (.der extension).
3. Enter BIOS configuration
4. Switch Secure boot to setup mode (or custom mode or clear keys). It deletes PK (platform certificate) and allows to load DCS platform key.
5. Boot Windows
6. Edit the file sb_set_siglists.ps1 and uncomment the lines related to the manufacturer of the machine and which reference the certfiicates names gethered from step 2. 
5. execute from admin command prompt
   powershell -ExecutionPolicy Bypass -File sb_set_siglists.ps1
It sets in PK (platform key) - DCS_platform
It sets in KEK (key exchange key) - DCS_key_exchange
It sets in db - DCS_sign MicWinProPCA2011_2011-10-19 MicCorUEFCA2011_2011-06-27 and the other certificates specific to your machine.

All DCS modules are protected by DCS_sign. 
All Windows modules are protected by MicWinProPCA2011_2011-10-19
All SHIM(linux) modules are protected by MicCorUEFCA2011_2011-06-27