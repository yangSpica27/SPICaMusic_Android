cmake_minimum_required(VERSION 4.0.2)

project(app)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -fvisibility=hidden -flto -fdata-sections -ffunction-sections -fomit-frame-pointer")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -fvisibility=hidden -flto -fdata-sections -ffunction-sections -fomit-frame-pointer")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -flto -Wl,--exclude-libs,ALL -Wl,--gc-sections -s -Wl,--pack-dyn-relocs=android -Wl,--build-id=none -Wl,-z,max-page-size=16384")

set(VISIBILITY_HIDDEN ON)
set(BUILD_BINDINGS OFF)
set(BUILD_TESTING OFF)


add_subdirectory(taglib)

include_directories("${CMAKE_CURRENT_SOURCE_DIR}")

# 扫描taglib目录下的所有文件，并将它们的路径存储在FILES变量中
file(GLOB_RECURSE FILES "${CMAKE_CURRENT_SOURCE_DIR}/taglib/*")

# 遍历FILES中的所有文件，提取出它们的目录，并将这些目录使用include_directories导入搜索路径中
foreach (FILE ${FILES})
    get_filename_component(DIR ${FILE} PATH)

    if (IS_DIRECTORY ${DIR})
        include_directories(${DIR})
    endif ()
endforeach ()

add_library(
        taglib SHARED
        taglibWrapper.cpp
)

find_library(log-lib log)
target_link_libraries(taglib tag ${log-lib})