cmake_minimum_required(VERSION 3.14)
project(gopro_gpmf)

set(CMAKE_CXX_STANDARD 14)

# FIXME should not add /usr/local/include explicitely
include_directories(/usr/local/include)

set(CMAKE_CXX_IMPLICIT_LINK_DIRECTORIES /usr/local/lib "${CMAKE_CXX_IMPLICIT_LINK_DIRECTORIES}")

add_executable(gopro_gpmf main.cpp GPMF_mp4reader.c)

# FIXME should not specify library location
target_link_libraries(gopro_gpmf /usr/local/lib/libgpmf-parser.a)
