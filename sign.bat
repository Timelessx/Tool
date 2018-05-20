@color 0b
::jarsigner -verbose -certs -keystore *.keystore -storepass %1 -keypass %2 -digestalg SHA1 -sigalg MD5withRSA -sigfile CERT -signedjar %~n3_signed.apk %3 %4
jarsigner -verbose -certs -keystore o.o -storepass o.o -keypass o.o -digestalg SHA1 -sigalg SHA1withRSA -sigfile CERT -signedjar %~n1_signed.apk %1 alias
@zipalign -f -v 4 %~n1_signed.apk %~n1_%date:~0,4%%date:~5,2%%date:~8,2%%time:~0,2%%time:~3,2%%time:~6,2%_signed.apk
@del /f %~n1_signed.apk
@pause>nul
