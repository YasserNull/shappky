package com.yn.shappky;

interface IShizukuShellService {
    String runCommand(String command) = 1;
    void destroy() = 16777114;
}
