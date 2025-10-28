/**
 * Renders mathematical expressions using MathJax
 * This function is called after displaying quiz results to ensure proper rendering of math content
 */
function renderMathJax() {
    if (window.MathJax) {
        MathJax.typesetPromise && MathJax.typesetPromise();
    }
}

// Initialize quiz results when the page loads
document.addEventListener('DOMContentLoaded', async () => {
    try {
        // Get quiz ID from URL parameters
        const urlParams = new URLSearchParams(window.location.search);
        const quizId = urlParams.get('quizId');
        
        // Validate quiz ID
        if (!quizId) {
            throw new Error('No quiz ID provided');
        }

        console.log('Fetching results for quiz:', quizId);

        // Fetch quiz results from the server
        const response = await fetch(`/api/quizzes/${quizId}/results`, {
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`,
                'Content-Type': 'application/json'
            }
        });

        // Handle error response
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to load quiz results');
        }

        // Display the results
        const results = await response.json();
        console.log('Received results:', results);
        displayResults(results);

    } catch (error) {
        // Display error message and provide option to return to dashboard
        console.error('Error details:', error);
        document.getElementById('questionReview').innerHTML = `
            <div class="error-message">
                <p>Failed to load quiz results: ${error.message}</p>
                <button onclick="window.location.href='/dashboard.html'" class="btn primary">
                    Back to Dashboard
                </button>
            </div>
        `;
    }
});

/**
 * Displays the quiz results, including questions, answers, and scoring
 * @param {Object} results - The quiz results object containing questions and scores
 */
function displayResults(results) {
    // Validate results data
    if (!results || !results.questions) {
        document.getElementById('questionReview').innerHTML = `
            <div class="error-message">
                <p>No quiz results available</p>
                <button onclick="window.location.href='/dashboard.html'" class="btn primary">
                    Back to Dashboard
                </button>
            </div>
        `;
        return;
    }

    // Initialize score display
    document.getElementById('score').textContent = 'Review your answers and assign points below';
    
    // Display time spent on quiz
    const timeSpent = calculateTimeSpent(results.startTime, results.endTime);
    document.getElementById('timeSpent').textContent = `Time spent: ${timeSpent}`;

    // Check if quiz has been graded
    const isGraded = results.questions.some(q => 
        (q.pointsEarned !== null && q.pointsEarned !== undefined) || 
        (q.subPointsEarned && q.subPointsEarned.length > 0)
    );

    // Generate HTML for each question review
    const reviewHtml = results.questions.map((question, index) => {
        // Determine score status and visual indicator color
        let scoreStatus = 'not-graded';
        let borderColor = '#95a5a6'; // Default gray for not graded
        
        if (isGraded) {
            if (question.question.questionType === 'WITH_SUBSECTIONS') {
                // Calculate total points for subsection questions
                const totalEarned = question.subPointsEarned ? 
                    question.subPointsEarned.reduce((sum, p) => sum + p, 0) : 0;
                const totalPossible = question.question.subQuestions.reduce((sum, q) => sum + q.points, 0);
                
                // Set status based on points earned
                if (totalEarned === 0) {
                    scoreStatus = 'wrong';
                    borderColor = '#e74c3c'; // Red for wrong
                } else if (totalEarned === totalPossible) {
                    scoreStatus = 'correct';
                    borderColor = '#2ecc71'; // Green for correct
                } else {
                    scoreStatus = 'partial';
                    borderColor = '#f1c40f'; // Yellow for partial
                }
            } else {
                // Handle non-subsection questions
                const earnedPoints = question.pointsEarned || 
                    (question.subPointsEarned && question.subPointsEarned.reduce((sum, p) => sum + p, 0)) || 0;
                const totalPoints = question.question.points || 0;
                
                // Set status based on points earned
                if (earnedPoints === 0) {
                    scoreStatus = 'wrong';
                    borderColor = '#e74c3c';
                } else if (earnedPoints === totalPoints) {
                    scoreStatus = 'correct';
                    borderColor = '#2ecc71';
                } else {
                    scoreStatus = 'partial';
                    borderColor = '#f1c40f';
                }
            }
        }

        // Base HTML for question display
        const baseQuestionHtml = `<h4>Question ${index + 1}</h4>
<p class="question-text">${question.question.questionText}</p>`;

        // Handle different question types
        if (question.question.questionType === 'MULTIPLE_CHOICE') {
            // Process multiple choice questions
            const correctOption = question.question.options.find(opt => opt.correct);
            const correctOptionText = correctOption ? correctOption.optionText : 'No correct answer specified';
            const isCorrect = question.userAnswer === correctOptionText;
            const points = isCorrect ? question.question.points : 0;

            return `
                <div class="question-review ${points > 0 ? 'correct' : 'wrong'}" style="border-left: 4px solid ${points > 0 ? '#2ecc71' : '#e74c3c'}">
                    ${baseQuestionHtml}
                    <p><strong>Your Answer:</strong> ${question.userAnswer || 'No answer provided'}</p>
                    <p><strong>Mark Scheme:</strong> ${correctOptionText}</p>
                    <p><strong>Points:</strong> ${points}/${question.question.points}</p>
                </div>`;
        } else if (question.question.questionType === 'WITH_SUBSECTIONS') {
            // Process questions with subsections
            const subQuestionsHtml = question.question.subQuestions.map((subQ, subIndex) => {
                const userAnswer = question.subAnswers && question.subAnswers[subIndex] 
                    ? question.subAnswers[subIndex] 
                    : 'No answer provided';
                const earnedPoints = question.subPointsEarned && question.subPointsEarned[subIndex] 
                    ? question.subPointsEarned[subIndex] 
                    : 0;
                    
                return `
                    <div class="sub-question">
                        <h5>${subQ.subIdentifier}) ${subQ.questionText}</h5>
                        <p><strong>Your Answer:</strong> ${userAnswer}</p>
                        <p><strong>Mark Scheme:</strong> ${subQ.markScheme}</p>
                        <div class="points-section">
                            <label>Points (max ${subQ.points}):</label>
                            <input type="number" 
                                   class="points-input" 
                                   data-question-id="${question.id}"
                                   data-sub-index="${subIndex}"
                                   min="0" 
                                   max="${subQ.points}"
                                   value="${earnedPoints}"
                                   ${isGraded ? 'disabled' : ''}
                                   onchange="updateTotalScore()">
                        </div>
                    </div>`;
            }).join('');

            return `
                <div class="question-review subsection-type ${scoreStatus}" style="border-left: 4px solid ${borderColor}">
                    ${baseQuestionHtml}
                    ${subQuestionsHtml}
                </div>`;
        } else {
            // Process standard questions
            const earnedPoints = question.pointsEarned || 
                (question.subPointsEarned && question.subPointsEarned[0]) || 0;
            
            return `
                <div class="question-review standard-type ${scoreStatus}" style="border-left: 4px solid ${borderColor}">
                    ${baseQuestionHtml}
                    <p><strong>Your Answer:</strong> ${question.userAnswer || 'No answer provided'}</p>
                    <p><strong>Mark Scheme:</strong> ${question.question.markScheme}</p>
                    <div class="points-section">
                        <label>Points (max ${question.question.points}):</label>
                        <input type="number" 
                               class="points-input" 
                               data-question-id="${question.id}"
                               min="0" 
                               max="${question.question.points}"
                               value="${earnedPoints}"
                               ${isGraded ? 'disabled' : ''}
                               onchange="updateTotalScore()">
                    </div>
                </div>`;
        }
    }).join('');

    // Update the page with question reviews
    document.getElementById('questionReview').innerHTML = reviewHtml;
    
    // Clear existing buttons
    const existingButtons = document.querySelectorAll('.button-container');
    existingButtons.forEach(button => button.remove());
    
    // Create new button container
    const buttonContainer = document.createElement('div');
    buttonContainer.className = 'button-container';
    
    // Add appropriate button based on grading status
    if (!isGraded) {
        buttonContainer.innerHTML = `
            <button onclick="saveGrades()" class="btn primary">Save Grades</button>
        `;
    } else {
        buttonContainer.innerHTML = `
            <button onclick="window.location.href='/dashboard.html'" class="btn primary">Back to Dashboard</button>
        `;
    }
    
    // Add button container to page
    document.getElementById('questionReview').after(buttonContainer);
    updateTotalScore();
    renderMathJax();
}

/**
 * Updates the total score display based on points earned
 * Calculates points from both multiple choice and manually graded questions
 */
function updateTotalScore() {
    // Calculate points from multiple choice questions
    const mcPoints = Array.from(document.querySelectorAll('.question-review.correct'))
        .reduce((sum, div) => {
            const pointsText = div.querySelector('strong').nextSibling.textContent;
            return sum + parseInt(pointsText);
        }, 0);
    
    // Calculate points from manual inputs
    const manualPoints = Array.from(document.querySelectorAll('.points-input'))
        .reduce((sum, input) => sum + (parseInt(input.value) || 0), 0);
    
    // Update total score display
    const totalScore = mcPoints + manualPoints;
    document.getElementById('score').textContent = `Score: ${totalScore}`;
}

/**
 * Saves the grades for all questions to the server
 * Collects points from both standard and subsection questions
 */
async function saveGrades() {
    const questionPoints = {};
    const urlParams = new URLSearchParams(window.location.search);
    const quizId = urlParams.get('quizId');
    
    // Collect points from all question inputs
    document.querySelectorAll('.question-review').forEach(questionDiv => {
        const inputs = questionDiv.querySelectorAll('.points-input');
        if (inputs.length > 0) {
            const questionId = inputs[0].dataset.questionId;
            if (questionDiv.classList.contains('standard-type')) {
                // Handle standard questions
                questionPoints[questionId] = [parseInt(inputs[0].value) || 0];
            } else {
                // Handle subsection questions
                const points = Array.from(inputs).map(input => parseInt(input.value) || 0);
                questionPoints[questionId] = points;
            }
        }
    });

    try {
        const response = await fetch(`/api/quizzes/${quizId}/grade`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(questionPoints)
        });

        if (!response.ok) {
            throw new Error('Failed to save grades');
        }

        alert('Grades saved successfully!');
        // Reload the page to show the Back to Dashboard button
        window.location.reload();
    } catch (error) {
        console.error('Error:', error);
        alert('Failed to save grades: ' + error.message);
    }
}

function calculateTimeSpent(startTime, endTime) {
    const start = new Date(startTime);
    const end = new Date(endTime);
    const diff = Math.floor((end - start) / 1000); // difference in seconds
    
    const minutes = Math.floor(diff / 60);
    const seconds = diff % 60;
    
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

function showQuestionDetails(question) {
    // ... existing code ...
    renderMathJax();
}