// Global variables for quiz state
let quiz = null;                    // Stores the current quiz data
let currentQuestionIndex = 0;       // Tracks the current question being displayed
let timer = null;                   // Timer for quiz duration

/**
 * Renders mathematical expressions using MathJax
 * This function is called after displaying questions to ensure proper rendering of math content
 */
function renderMathJax() {
    if (window.MathJax) {
        MathJax.typesetPromise && MathJax.typesetPromise();
    }
}

// Initialize quiz when the page loads
document.addEventListener('DOMContentLoaded', async () => {
    try {
        // Get quiz ID from URL parameters
        const urlParams = new URLSearchParams(window.location.search);
        const quizId = urlParams.get('quizId');
        
        // Validate quiz ID
        if (!quizId) {
            throw new Error('No quiz ID provided');
        }

        // Fetch quiz data from server
        const response = await fetch(`/api/quizzes/${quizId}`, {
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`,
                'Content-Type': 'application/json'
            }
        });

        // Handle error response
        if (!response.ok) {
            throw new Error('Failed to load quiz');
        }

        // Process quiz data
        quiz = await response.json();
        console.log('Quiz data:', quiz);
        
        // Validate quiz data
        if (!quiz || !quiz.timeLimit) {
            throw new Error('Invalid quiz data received');
        }

        // Initialize quiz interface
        initializeQuiz(quiz);
        startTimer(quiz.timeLimit);
        createQuestionNavigator();
        if (quiz.quizQuestions && quiz.quizQuestions.length > 0) {
            displayQuestion(quiz.quizQuestions[0]);
        }

    } catch (error) {
        console.error('Error:', error);
        alert('Failed to load quiz: ' + error.message);
    }
});

/**
 * Starts the quiz timer
 * @param {number} minutes - The time limit in minutes
 */
function startTimer(minutes) {
    const endTime = new Date().getTime() + minutes * 60000;
    
    // Update timer every second
    timer = setInterval(() => {
        const now = new Date().getTime();
        const distance = endTime - now;
        
        // Handle quiz timeout
        if (distance < 0) {
            clearInterval(timer);
            submitQuiz(true);
            return;
        }
        
        // Calculate and display remaining time
        const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((distance % (1000 * 60)) / 1000);
        
        document.getElementById('timeRemaining').textContent = 
            `Time Remaining: ${minutes}:${seconds.toString().padStart(2, '0')}`;
    }, 1000);
}

/**
 * Initializes the quiz interface with initial values
 * @param {Object} quiz - The quiz data object
 */
function initializeQuiz(quiz) {
    const timeLimit = quiz.timeLimit || 0;
    const formattedTime = `${timeLimit}:00`;
    
    // Set initial time display
    document.getElementById('timeRemaining').textContent = `Time Remaining: ${formattedTime}`;
    
    // Set initial question counter
    const totalQuestions = quiz.quizQuestions ? quiz.quizQuestions.length : 0;
    document.getElementById('questionCounter').textContent = `Question 1 of ${totalQuestions}`;
}

/**
 * Displays the current question with its content and answer input
 * @param {Object} questionData - The question data object
 */
function displayQuestion(questionData) {
    const questionContent = document.getElementById('questionContent');
    if (!questionContent || !questionData || !questionData.question) return;

    // Generate HTML for question display
    const html = `
        <div class="question">
            <div class="question-header">
                <h3>Question ${currentQuestionIndex + 1}</h3>
                <button onclick="toggleFlag(${currentQuestionIndex})" 
                        class="flag-btn ${questionData.flaggedForReview ? 'flagged' : ''}">
                    ${questionData.flaggedForReview ? 'Flagged' : 'Flag for Review'}
                </button>
            </div>
            <p>${questionData.question.questionText}</p>
            ${renderAnswerInput(questionData)}
        </div>
    `;

    questionContent.innerHTML = html;
    renderMathJax();
}

/**
 * Renders the appropriate answer input based on question type
 * @param {Object} questionData - The question data object
 * @returns {string} HTML string for the answer input
 */
function renderAnswerInput(questionData) {
    if (questionData.question.questionType === 'MULTIPLE_CHOICE') {
        // Render multiple choice options
        return `
            <div class="options">
                ${questionData.question.options.map((option, index) => `
                    <label class="option">
                        <input type="radio" 
                               name="question-${questionData.id}" 
                               value="${option.optionText}"
                               ${questionData.userAnswer === option.optionText ? 'checked' : ''}
                               onchange="saveAnswer(${currentQuestionIndex}, this.value)">
                        ${option.optionText}
                    </label>
                `).join('')}
            </div>
        `;
    } else if (questionData.question.questionType === 'WITH_SUBSECTIONS') {
        // Render subsection questions with text areas
        return `
            <div class="sub-questions">
                ${questionData.question.subQuestions.map((subQ, index) => `
                    <div class="sub-question">
                        <div class="sub-question-header">
                            <h4>${String.fromCharCode(97 + index)}) ${subQ.questionText}</h4>
                            <div class="sub-question-points">(${subQ.points} points)</div>
                        </div>
                        <div class="sub-question-content">
                            <textarea 
                                class="answer-input"
                                placeholder="Enter your answer here"
                                onchange="saveSubAnswer(${currentQuestionIndex}, ${index}, this.value)"
                            >${questionData.subAnswers?.[index] || ''}</textarea>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    } else {
        // Render standard question with text area
        return `
            <div class="standard-question">
                <textarea 
                    class="answer-input"
                    placeholder="Enter your answer here"
                    onchange="saveAnswer(${currentQuestionIndex}, this.value)"
                >${questionData.userAnswer || ''}</textarea>
            </div>
        `;
    }
}

/**
 * Navigates to the previous question
 */
function previousQuestion() {
    if (currentQuestionIndex > 0) {
        currentQuestionIndex--;
        displayQuestion(quiz.quizQuestions[currentQuestionIndex]);
        updateQuestionCounter();
        updateNavigator();
    }
}

/**
 * Navigates to the next question
 */
function nextQuestion() {
    if (currentQuestionIndex < quiz.quizQuestions.length - 1) {
        currentQuestionIndex++;
        displayQuestion(quiz.quizQuestions[currentQuestionIndex]);
        updateQuestionCounter();
        updateNavigator();
    }
}

/**
 * Updates the question counter display
 */
function updateQuestionCounter() {
    const counter = document.getElementById('questionCounter');
    if (counter && quiz.quizQuestions) {
        counter.textContent = `Question ${currentQuestionIndex + 1} of ${quiz.quizQuestions.length}`;
    }
}

/**
 * Submits the quiz to the server
 * @param {boolean} isTimeout - Whether the submission is due to timeout
 */
async function submitQuiz(isTimeout = false) {
    if (!isTimeout && !confirm('Are you sure you want to submit the quiz?')) {
        return;
    }

    clearInterval(timer);
    
    // Async function to handle quiz submission
    // isTimeout parameter indicates if submission was triggered by timer expiration

    try {
        // Send the quiz answers to the server
        const response = await fetch(`/api/quizzes/${quiz.id}/submit`, {
            method: 'POST',                      // HTTP method
            headers: {
                'Content-Type': 'application/json',              // JSON content type
                'Authorization': `Bearer ${localStorage.getItem('token')}`                // Authentication token
            },
            body: JSON.stringify(quiz.quizQuestions.map(q => ({
                id: q.id,
                userAnswer: q.userAnswer || null,
                subAnswers: q.subAnswers || [],
                question: {
                    id: q.question.id,
                    questionType: q.question.questionType
                }
            })))              // Convert answers to JSON
        });
        
        // Handle the server response
        if (response.ok) {
            // If submission successful, redirect to results page
            const result = await response.json();
            window.location.href = `/quiz-results.html?quizId=${quiz.id}`;
        } else {
            // If submission failed, show error message
            const errorData = await response.text();
            throw new Error(errorData);
        }
    } catch (error) {
        // Handle any exceptions that occurred during submission
        console.error('Error submitting quiz:', error);
        alert('Failed to submit quiz: ' + error.message);
    }
}

/**
 * Saves a user's answer for a question
 * @param {number} questionIndex - The index of the question
 * @param {string} value - The answer value
 */
function saveAnswer(questionIndex, value) {
    if (quiz && quiz.quizQuestions) {
        quiz.quizQuestions[questionIndex].userAnswer = value;
        console.log('Saved answer:', value, 'for question:', questionIndex);
    }
}

/**
 * Saves points earned for a question
 * @param {number} questionIndex - The index of the question
 * @param {number} points - The points earned
 */
function savePoints(questionIndex, points) {
    if (quiz && quiz.quizQuestions) {
        quiz.quizQuestions[questionIndex].pointsEarned = parseInt(points);
        console.log('Saved points:', points, 'for question:', questionIndex);
    }
}

/**
 * Creates the question navigation interface
 */
function createQuestionNavigator() {
    const nav = document.getElementById('questionNav');
    if (!nav || !quiz.quizQuestions) return;

    // Generate navigation buttons for each question
    nav.innerHTML = `
        <div class="nav-buttons">
            ${quiz.quizQuestions.map((q, index) => `
                <button onclick="goToQuestion(${index})" 
                        class="nav-dot ${index === currentQuestionIndex ? 'active' : ''} ${q.flaggedForReview ? 'flagged' : ''}"
                        id="nav-${index}">
                    ${index + 1}
                </button>
            `).join('')}
        </div>
    `;
}

/**
 * Toggles the flag status of a question for review
 * @param {number} index - The index of the question
 */
function toggleFlag(index) {
    if (!quiz.quizQuestions) return;
    
    const question = quiz.quizQuestions[index];
    question.flaggedForReview = !question.flaggedForReview;
    
    // Update flag button text
    const flagBtn = document.querySelector('.flag-btn');
    if (flagBtn) {
        flagBtn.textContent = question.flaggedForReview ? 'Flagged' : 'Flag for Review';
        flagBtn.classList.toggle('flagged', question.flaggedForReview);
    }
    
    // Update navigator dot
    const navDot = document.getElementById(`nav-${index}`);
    if (navDot) {
        navDot.classList.toggle('flagged', question.flaggedForReview);
    }
}

/**
 * Updates the question navigation interface
 */
function updateNavigator() {
    // Remove active class from all dots
    document.querySelectorAll('.nav-dot').forEach((dot, index) => {
        dot.classList.remove('active');
        if (index === currentQuestionIndex) {
            dot.classList.add('active');
        }
    });
}

/**
 * Navigates to a specific question
 * @param {number} index - The index of the question to navigate to
 */
function goToQuestion(index) {
    if (!quiz.quizQuestions || index < 0 || index >= quiz.quizQuestions.length) return;
    
    currentQuestionIndex = index;
    displayQuestion(quiz.quizQuestions[index]);
    updateQuestionCounter();
    updateNavigator();
}

/**
 * Saves a user's answer for a subsection question
 * @param {number} questionIndex - The index of the main question
 * @param {number} subQuestionIndex - The index of the subsection
 * @param {string} value - The answer value
 */
function saveSubAnswer(questionIndex, subQuestionIndex, value) {
    if (quiz && quiz.quizQuestions) {
        if (!quiz.quizQuestions[questionIndex].subAnswers) {
            quiz.quizQuestions[questionIndex].subAnswers = [];
        }
        quiz.quizQuestions[questionIndex].subAnswers[subQuestionIndex] = value;
        console.log('Saved sub-answer:', value, 'for question:', questionIndex, 'sub-question:', subQuestionIndex);
    }
}

/**
 * Loads and displays a specific question
 * @param {number} index - The index of the question to load
 */
function loadQuestion(index) {
    // ... existing code ...
    renderMathJax();
}

/**
 * Displays feedback for the current question
 */
function showFeedback() {
    // ... existing code ...
    renderMathJax();
}