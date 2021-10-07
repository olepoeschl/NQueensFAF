On Error Resume Next
WScript.Sleep(5000)
nqfaf_cleanup_folders = CreateObject("WScript.Shell").ExpandEnvironmentStrings("%Temp%") & "\NQueensFaf*"
CreateObject("Scripting.FileSystemObject").DeleteFolder(nqfaf_cleanup_folders)
Set oFso = CreateObject("Scripting.FileSystemObject") : oFso.DeleteFile Wscript.ScriptFullName, True 