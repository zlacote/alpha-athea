cmake_minimum_required(VERSION 3.22.1)
project(ayana LANGUAGES C)

add_library(ayana SHARED pty.c)
target_compile_options(ayana PRIVATE -O3 -flto -ffunction-sections -fdata-sections)
target_link_options(ayana PRIVATE -Wl,--gc-sections -s -flto)
