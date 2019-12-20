// IMassageService.aidl
package android.os;

// Declare any non-default types here with import statements

interface IMassageService {
    void installPackage(String installerPackageName,String installedPackageName,String packagePath,boolean autoStart);
    void clearPackage(String packageName,boolean autoStart);
}
