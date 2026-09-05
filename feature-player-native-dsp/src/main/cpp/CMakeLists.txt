cmake_minimum_required(VERSION 3.22.1)

project(spica_native_dsp LANGUAGES C CXX)

set(CMAKE_C_STANDARD 99)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(THIRD_PARTY ${CMAKE_CURRENT_SOURCE_DIR}/third_party)
set(PFFFT_ROOT ${THIRD_PARTY}/pffft)
set(EBUR128_ROOT ${THIRD_PARTY}/libebur128)
set(DSPFILTERS_ROOT ${THIRD_PARTY}/dspfilters)

add_library(pffft STATIC
    ${PFFFT_ROOT}/src/pffft.c
    ${PFFFT_ROOT}/src/pffft_common.c
)
target_include_directories(pffft
    PUBLIC ${PFFFT_ROOT}/include
    PRIVATE ${PFFFT_ROOT}/include/pffft
    PRIVATE ${PFFFT_ROOT}/src
)
target_compile_definitions(pffft PRIVATE
    PFFFT_ENABLE_NEON=1
    PFFFT_STATIC_DEFINE=1
)
set_target_properties(pffft PROPERTIES POSITION_INDEPENDENT_CODE ON)

add_library(ebur128 STATIC ${EBUR128_ROOT}/ebur128.c)
target_include_directories(ebur128 PUBLIC ${EBUR128_ROOT})
set_target_properties(ebur128 PROPERTIES POSITION_INDEPENDENT_CODE ON)

file(GLOB DSPFILTERS_SOURCES CONFIGURE_DEPENDS "${DSPFILTERS_ROOT}/source/*.cpp")
add_library(dspfilters STATIC ${DSPFILTERS_SOURCES})
target_include_directories(dspfilters PUBLIC ${DSPFILTERS_ROOT}/include)
set_target_properties(dspfilters PROPERTIES POSITION_INDEPENDENT_CODE ON)

add_library(spica_native_dsp SHARED
    src/dsp_engine.cpp
    src/jni_bridge.cpp
)

target_include_directories(spica_native_dsp PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${PFFFT_ROOT}/include
    ${EBUR128_ROOT}
    ${DSPFILTERS_ROOT}/include
)

target_compile_definitions(spica_native_dsp PRIVATE
    PFFFT_STATIC_DEFINE=1
)

target_compile_options(spica_native_dsp PRIVATE
    -Wall
    -Wextra
    -Wpedantic
    -Wno-unused-parameter
)

target_link_libraries(spica_native_dsp
    pffft
    ebur128
    dspfilters
    log
)

set_target_properties(spica_native_dsp PROPERTIES
    CXX_VISIBILITY_PRESET hidden
    VISIBILITY_INLINES_HIDDEN ON
)
