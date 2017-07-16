# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# configure import libs
set(distribution_DIR ${CMAKE_SOURCE_DIR}/../ffmpeg-android)

add_library(lib_avformat STATIC IMPORTED)
set_target_properties(lib_avformat PROPERTIES IMPORTED_LOCATION
    ${distribution_DIR}/${ANDROID_ABI}/lib/libavformat.a)

add_library(lib_avcodec STATIC IMPORTED)
set_target_properties(lib_avcodec PROPERTIES IMPORTED_LOCATION
    ${distribution_DIR}/${ANDROID_ABI}/lib/libavcodec.a)

add_library(lib_swscale STATIC IMPORTED)
set_target_properties(lib_swscale PROPERTIES IMPORTED_LOCATION
    ${distribution_DIR}/${ANDROID_ABI}/lib/libswscale.a)
add_library(lib_avutil STATIC IMPORTED)
set_target_properties(lib_avutil PROPERTIES IMPORTED_LOCATION
    ${distribution_DIR}/${ANDROID_ABI}/lib/libavutil.a)	
# now build app's shared lib
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall")

add_library(native-lib SHARED
             src/main/cpp/native-lib.cpp
			 src/main/cpp/RingBuffer.cpp
			 src/main/cpp/vdecoder.cpp
			 src/main/cpp/playback.cpp
			 )
			 
target_include_directories(native-lib PRIVATE
	${distribution_DIR}/include
	)

				 
 

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )


# add lib dependencies
target_link_libraries(native-lib
                      android
                      ${log-lib} 
                      EGL
					  lib_avformat
					  lib_avcodec
					  lib_swscale
					  lib_avutil
                      GLESv2)
