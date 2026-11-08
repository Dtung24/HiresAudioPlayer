cmake_minimum_required(VERSION 3.22.1)

project("aether_dsp")

add_library(
    aether_dsp
    SHARED
    native-lib.cpp
    dsp/BiquadFilter.cpp
    dsp/ParametricEqualizer.cpp
    dsp/GraphicEqualizer.cpp
    dsp/SincResampler.cpp
    dsp/HarmonicExciter.cpp
    dsp/CrossfeedFilter.cpp
    dsp/FftAnalyzer.cpp
    dsp/AudioPipeline.cpp
)

target_include_directories(
    aether_dsp
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/dsp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    aether_dsp
    ${log-lib}
    m
)

