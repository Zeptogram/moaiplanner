package com.example.moaiplanner.ui.utils

class CourseModel (private var course_name: String, private var course_rating: Int, private var course_image: Int) {

    // Getter and Setter
    fun getCourse_name(): String {
        return course_name
    }

    fun setCourse_name(course_name: String) {
        this.course_name = course_name
    }

    fun getCourse_rating(): Int {
        return course_rating
    }

    fun setCourse_rating(course_rating: Int) {
        this.course_rating = course_rating
    }

    fun getCourse_image(): Int {
        return course_image
    }

    fun setCourse_image(course_image: Int) {
        this.course_image = course_image
    }
}

