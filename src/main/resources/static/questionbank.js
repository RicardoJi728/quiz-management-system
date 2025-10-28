// Token handling at the top
const token = localStorage.getItem('token');
if (!token) {
    localStorage.clear();
    window.location.href = '/';
}

// Get currentBankId from URL parameters
const urlParams = new URLSearchParams(window.location.search);
const currentBankId = urlParams.get('bankId') || urlParams.get('id');

// Initialize form handlers
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('questionForm');
    // Remove any existing listeners
    form.replaceWith(form.cloneNode(true));
    // Get the fresh form reference
    const newForm = document.getElementById('questionForm');
    // Add single listener
    newForm.addEventListener('submit', handleAddQuestion);
    renderMathJax();
});

function validateQuestionForm() {
    const questionText = document.getElementById('questionText').value;
    const points = document.getElementById('points').value;
    const level = document.getElementById('level').value;
    const questionType = document.getElementById('questionType').value;

    if (!questionText || !points || !level) {
        alert('Please fill in all required fields');
        return false;
    }

    // Only require mark scheme for non-multiple choice questions
    if (questionType !== 'MULTIPLE_CHOICE') {
        const markScheme = document.getElementById('markScheme').value;
        if (!markScheme) {
            alert('Please provide a mark scheme');
            return false;
        }
    }

    return true;
}

// Add question form handler
document.getElementById('questionForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    if (!validateQuestionForm()) {
        return;
    }

    const questionData = {
        questionText: document.getElementById('questionText').value,
        points: parseInt(document.getElementById('points').value),
        level: document.getElementById('level').value,
        questionType: document.getElementById('questionType').value,
        bank: { id: parseInt(currentBankId) },
        markScheme: document.getElementById('questionType').value !== 'MULTIPLE_CHOICE' 
            ? document.getElementById('markScheme').value 
            : null,
    };

    // Add topic if selected
    const topicSelect = document.getElementById('topicSelect');
    if (topicSelect && topicSelect.value) {
        questionData.topic = { id: parseInt(topicSelect.value) };
    }

    try {
        const response = await fetch('/api/questions', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(questionData)
        });

        if (!response.ok) {
            throw new Error('Failed to create question');
        }

        // Refresh both questions list and topics section
        await Promise.all([
            loadQuestions(),     // Refresh questions
            loadTopicsSection()  // Refresh topics with updated counts
        ]);
        
        // Reset the form
        document.getElementById('questionForm').reset();
        
        // Reset any dynamic form elements
        handleQuestionTypeChange();

    } catch (error) {
        console.error('Error:', error);
        alert('Failed to create question: ' + error.message);
    }
});

async function loadBankDetails() {
    try {
        const response = await fetch(`/api/questionbanks/${currentBankId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (!response.ok) throw new Error('Failed to load bank details');
        
        const bank = await response.json();
        document.getElementById('bankTitle').textContent = bank.title;
        
        // Update header buttons to include publish toggle
        const headerButtons = document.querySelector('.header-buttons');
        if (headerButtons) {
            headerButtons.innerHTML = `
                <button onclick="createQuiz()" class="primary-btn">Create Quiz</button>
                <button onclick="togglePublish(${currentBankId})" class="publish-btn ${bank.published ? 'published' : ''}">
                    ${bank.published ? 'Unpublish' : 'Publish'}
                </button>
                <button onclick="window.location.href='dashboard.html'" class="back-btn">Back to Dashboard</button>
            `;
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Failed to load bank details: ' + error.message);
    }
}

async function loadQuestions() {
    try {
        const response = await fetch(`/api/questions/bank/${currentBankId}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            if (response.status === 401 || response.status === 403) {
                localStorage.clear();
                window.location.href = '/';
                return;
            }
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const questions = await response.json();
        displayQuestions(questions);
    } catch (error) {
        console.error('Error loading questions:', error);
        alert('Error loading questions: ' + error.message);
    }
}

function displayQuestions(questions) {
    const questionsList = document.getElementById('questionsList');
    
    // Check if questions array is empty
    if (!questions || questions.length === 0) {
        questionsList.innerHTML = `
            <div class="no-questions-message">
                <p>No questions have been added to this question bank yet.</p>
                <p>Use the form above to add your first question!</p>
            </div>
        `;
        return;
    }

    questionsList.innerHTML = questions.map(q => {
        console.log('Displaying question:', q); // Debug log
        
        // Calculate total points for questions with subsections
        let totalPoints = q.points || 0;
        if (q.questionType === 'WITH_SUBSECTIONS' && q.subQuestions) {
            totalPoints = q.subQuestions.reduce((sum, sub) => sum + (sub.points || 0), 0);
        }

        return `
            <div class="question-item">
                <div class="question-sidebar">
                    <button onclick="editQuestion(${q.id})" class="action-btn edit-btn">Edit</button>
                    <button onclick="toggleInfo(${q.id})" class="action-btn info-btn">Info</button>
                    <button onclick="deleteQuestion(${q.id})" class="action-btn delete-btn">Delete</button>
                </div>

                <div class="question-main">
                    <div class="question-header">
                        <div class="question-meta">
                            <span class="question-type-badge">${q.questionType}</span>
                            <span class="level-badge level-${q.level}">${q.level}</span>
                            <span class="points-badge">${totalPoints} points</span>
                            ${q.topic ? `<span class="topic-badge">${q.topic.name}</span>` : ''}
                        </div>
                    </div>

                    <div class="question-body">
                        <div class="question-text">
                            ${q.questionType === 'WITH_SUBSECTIONS' ? `
                                <div class="main-question">
                                    <p>${q.questionText}</p>
                                </div>
                                <div class="sub-questions">
                                    ${q.subQuestions ? q.subQuestions.map((sub, index) => `
                                        <div class="sub-question-item">
                                            <p class="sub-question-text">${String.fromCharCode(97 + index)}) ${sub.questionText}</p>
                                            <div class="mark-scheme">Mark Scheme: ${sub.markScheme}</div>
                                            <div class="sub-points">Points: ${sub.points}</div>
                                        </div>
                                    `).join('') : ''}
                                </div>
                            ` : `
                                <p>${q.questionText}</p>
                            `}
                        </div>

                        ${q.questionType === 'MULTIPLE_CHOICE' && q.options && q.options.length > 0 ? `
                            <div class="multiple-choice-options">
                                ${q.options.map((option, index) => `
                                    <div class="option-line ${option.correct ? 'correct-option' : ''}">
                                        ${String.fromCharCode(65 + index)}. ${option.optionText}
                                    </div>
                                `).join('')}
                            </div>
                        ` : ''}

                        ${q.questionType !== 'MULTIPLE_CHOICE' && q.questionType !== 'WITH_SUBSECTIONS' && q.markScheme ? `
                            <div class="mark-scheme">
                                <h4>Mark Scheme</h4>
                                <p>${q.markScheme}</p>
                            </div>
                        ` : ''}
                    </div>

                    <div id="info-${q.id}" class="info-panel" style="display: none;">
                        <div class="info-content">
                            ${q.publicationDate ? `
                                <div class="info-item">
                                    <span class="info-label">Published:</span>
                                    <span class="info-value">${new Date(q.publicationDate).toLocaleDateString()}</span>
                                </div>
                            ` : ''}
                            ${q.additionalInfo ? `
                                <div class="info-item">
                                    <span class="info-label">Additional Info:</span>
                                    <span class="info-value">${q.additionalInfo}</span>
                                </div>
                            ` : ''}
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join('');

    // Debug: Log the first question's data
    if (questions.length > 0) {
        console.log('First question:', questions[0]);
        if (questions[0].questionType === 'MULTIPLE_CHOICE') {
            console.log('Multiple choice options:', questions[0].options);
        }
    }

    // Render MathJax after updating the content
    renderMathJax();
}

function editQuestion(questionId) {
    const form = document.getElementById('questionForm');
    const newForm = form.cloneNode(true);
    form.replaceWith(newForm);
    
    const questions = document.querySelectorAll('.question-item');
    const questionElement = Array.from(questions).find(q => q.querySelector(`[onclick="editQuestion(${questionId})"]`));
    
    if (!questionElement) return;
    
    const questionType = questionElement.querySelector('.question-type-badge').textContent;
    const questionTypeSelect = newForm.querySelector('#questionType');
    questionTypeSelect.value = questionType;
    handleQuestionTypeChange();
    
    // Fill basic fields
    newForm.querySelector('#questionText').value = questionElement.querySelector('.main-question p')?.textContent.trim() || 
        questionElement.querySelector('.question-text p')?.textContent.trim();
    newForm.querySelector('#points').value = questionElement.querySelector('.points-badge')?.textContent.split(' ')[0] || '';
    newForm.querySelector('#level').value = questionElement.querySelector('.level-badge').textContent;
    
    // Fill publication date and additional info
    const infoPanel = document.getElementById(`info-${questionId}`);
    if (infoPanel) {
        const infoItems = infoPanel.querySelectorAll('.info-item');
        infoItems.forEach(item => {
            const label = item.querySelector('.info-label')?.textContent;
            const value = item.querySelector('.info-value')?.textContent;
            
            if (label && value) {
                if (label.includes('Published:')) {
                    const dateValue = new Date(value);
                    if (!isNaN(dateValue)) {
                        const formattedDate = dateValue.toISOString().split('T')[0];
                        newForm.querySelector('#publicationDate').value = formattedDate;
                    }
                }
                if (label.includes('Additional Info:')) {
                    newForm.querySelector('#additionalInfo').value = value.trim();
                }
            }
        });
    }
    
    // Set topic if it exists
    const topicBadge = questionElement.querySelector('.topic-badge');
    if (topicBadge) {
        const topicSelect = document.getElementById('topicSelect');
        // Wait for topics to load if necessary
        const checkTopicsLoaded = setInterval(() => {
            const topicOption = Array.from(topicSelect.options).find(option => 
                option.textContent === topicBadge.textContent
            );
            if (topicOption) {
                topicSelect.value = topicOption.value;
                clearInterval(checkTopicsLoaded);
            }
        }, 100);
        // Clear interval after 5 seconds to prevent infinite checking
        setTimeout(() => clearInterval(checkTopicsLoaded), 5000);
    }
    
    // Handle subsections
    if (questionType === 'WITH_SUBSECTIONS') {
        handleQuestionTypeChange(); // Ensure container exists
        
        const subQuestionsContainer = document.getElementById('subQuestionsContainer');
        if (subQuestionsContainer) {
            subQuestionsContainer.innerHTML = ''; // Clear existing subsections
            
            const subQuestions = questionElement.querySelectorAll('.sub-question-item');
            console.log('Found subquestions:', subQuestions);
            
            subQuestions.forEach((subQ, index) => {
                const fullText = subQ.querySelector('.sub-question-text').textContent;
                const subQuestionText = fullText.substring(fullText.indexOf(') ') + 2);
                
                const markSchemeElement = subQ.querySelector('.mark-scheme');
                const subQuestionMarkScheme = markSchemeElement.textContent.replace('Mark Scheme:', '').trim();
                
                const pointsElement = subQ.querySelector('.sub-points');
                const subQuestionPoints = parseInt(pointsElement.textContent.replace('Points:', '').trim());
                
                console.log('Adding subsection:', {
                    text: subQuestionText,
                    markScheme: subQuestionMarkScheme,
                    points: subQuestionPoints
                });
                
                addSubQuestion(subQuestionText, subQuestionMarkScheme, subQuestionPoints);
            });
        }
    }
    
    // Handle multiple choice options
    if (questionType === 'MULTIPLE_CHOICE') {
        const optionsList = document.getElementById('optionsList');
        optionsList.innerHTML = ''; // Clear existing options
        
        const options = questionElement.querySelectorAll('.option-line');
        options.forEach((option, index) => {
            const optionText = option.textContent.split('.')[1].split('(')[0].trim();
            const isCorrect = option.classList.contains('correct-option');
            
            const optionDiv = document.createElement('div');
            optionDiv.className = 'option';
            optionDiv.innerHTML = `
                <div class="option-row">
                    <span class="option-letter">${String.fromCharCode(65 + index)}.</span>
                    <input type="text" value="${optionText}" class="option-text" required>
                    <label class="correct-label">
                        <input type="radio" name="correctOption" value="${index}" ${isCorrect ? 'checked' : ''} required>
                        Correct Answer
                    </label>
                </div>
            `;
            optionsList.appendChild(optionDiv);
        });
    } else {
        newForm.querySelector('#markScheme').value = questionElement.querySelector('.mark-scheme p')?.textContent || '';
    }

    // Update form for edit mode
    newForm.dataset.editingQuestionId = questionId;
    newForm.addEventListener('submit', handleUpdateQuestion);
    
    // Update UI for edit mode
    const sectionTitle = document.querySelector('.section h2');
    sectionTitle.textContent = 'Edit Question';
    sectionTitle.style.color = '#2980b9';
    
    const submitButton = newForm.querySelector('button[type="submit"]');
    submitButton.textContent = 'Update Question';
    submitButton.style.backgroundColor = '#2980b9';
    
    if (!newForm.querySelector('.cancel-btn')) {
        const cancelButton = document.createElement('button');
        cancelButton.type = 'button';
        cancelButton.textContent = 'Cancel Edit';
        cancelButton.className = 'cancel-btn';
        cancelButton.onclick = resetForm;
        submitButton.parentNode.insertBefore(cancelButton, submitButton.nextSibling);
    }

    // Set topic if it exists
    const question = questions.find(q => q.id === questionId);
    if (question.topic) {
        document.getElementById('topicSelect').value = question.topic.id;
    }
}

async function handleUpdateQuestion(e) {
    e.preventDefault();
    e.stopPropagation(); // Add this to prevent form submission
    
    try {
        const form = e.target;
        const questionId = form.dataset.editingQuestionId;
        const questionType = document.getElementById('questionType').value;
        
        const updatedQuestion = {
            questionType: questionType,
            questionText: document.getElementById('questionText').value,
            level: document.getElementById('level').value,
            publicationDate: document.getElementById('publicationDate').value || null,
            additionalInfo: document.getElementById('additionalInfo').value || null,
            bank: { id: parseInt(currentBankId) },
            points: parseInt(document.getElementById('points').value),
            topic: document.getElementById('topicSelect').value ? 
                { id: parseInt(document.getElementById('topicSelect').value) } : 
                null
        };

        // Handle multiple choice options
        if (questionType === 'MULTIPLE_CHOICE') {
            const optionElements = document.querySelectorAll('.option');
            updatedQuestion.options = Array.from(optionElements).map(option => ({
                optionText: option.querySelector('.option-text').value,
                correct: option.querySelector('input[type="radio"]').checked
            }));
        }
        
        // Handle subsections
        if (questionType === 'WITH_SUBSECTIONS') {
            const subQuestions = Array.from(document.querySelectorAll('.sub-question')).map((sub, index) => ({
                subIdentifier: String.fromCharCode(97 + index),
                questionText: sub.querySelector('.sub-question-text').value,
                markScheme: sub.querySelector('.sub-question-mark-scheme').value,
                points: parseInt(sub.querySelector('.sub-question-points').value) || 0
            }));
            
            console.log('Updating with subsections:', subQuestions); // Debug log
            updatedQuestion.subQuestions = subQuestions;
            updatedQuestion.hasSubQuestions = true;
        } else {
            updatedQuestion.markScheme = document.getElementById('markScheme').value;
        }

        console.log('Sending update request:', updatedQuestion); // Debug log

        const response = await fetch(`/api/questions/${questionId}`, {
            method: 'PUT',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(updatedQuestion)
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Failed to update question');
        }

        await loadQuestions();
        resetForm();
        return false; // Add this to prevent form submission
    } catch (error) {
        console.error('Error:', error);
        alert('Failed to update question: ' + error.message);
        return false; // Add this to prevent form submission
    }
}

// Separate the add question handler
async function handleAddQuestion(e) {
    e.preventDefault();
    
    const questionType = document.getElementById('questionType').value;
    const questionData = {
        questionType: questionType,
        questionText: document.getElementById('questionText').value,
        level: document.getElementById('level').value,
        points: parseInt(document.getElementById('points').value),
        publicationDate: document.getElementById('publicationDate').value || null,
        additionalInfo: document.getElementById('additionalInfo').value || null,
        bank: { id: parseInt(currentBankId) },
        topic: document.getElementById('topicSelect').value ? 
            { id: parseInt(document.getElementById('topicSelect').value) } : 
            null
    };

    // Handle different question types
    if (questionType === 'MULTIPLE_CHOICE') {
        const options = Array.from(document.querySelectorAll('.option')).map(opt => ({
            optionText: opt.querySelector('.option-text').value,
            correct: opt.querySelector('input[type="radio"]').checked
        }));

        if (options.some(opt => !opt.optionText)) {
            alert('Please fill in all options');
            return;
        }

        questionData.options = options;
        questionData.markScheme = null;
    } else if (questionType === 'WITH_SUBSECTIONS') {
        const subQuestions = Array.from(document.querySelectorAll('.sub-question')).map((sub, index) => ({
            subIdentifier: String.fromCharCode(97 + index),
            questionText: sub.querySelector('.sub-question-text').value,
            markScheme: sub.querySelector('.sub-question-mark-scheme').value,
            points: parseInt(sub.querySelector('.sub-question-points').value) || 0
        }));

        if (subQuestions.length === 0) {
            alert('Please add at least one valid sub-question');
            return;
        }

        questionData.subQuestions = subQuestions;
        questionData.hasSubQuestions = true;
    } else {
        // Standard question type
        const markScheme = document.getElementById('markScheme').value;
        if (!markScheme) {
            alert('Please provide a mark scheme');
            return;
        }
        questionData.markScheme = markScheme;
        questionData.options = [];
        questionData.subQuestions = [];
    }

    try {
        const response = await fetch('/api/questions', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: JSON.stringify(questionData)
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        loadQuestions(); // Refresh the questions list
        resetForm(); // Reset the form
    } catch (error) {
        console.error('Error adding question:', error);
        alert('Failed to add question: ' + error.message);
    }
}

// Add this helper function to format the date
function formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toISOString().split('T')[0];
}

function resetForm() {
    const form = document.getElementById('questionForm');
    const newForm = form.cloneNode(true);
    form.replaceWith(newForm);
    
    newForm.reset();
    newForm.dataset.editingQuestionId = '';
    
    // Reset section title and style
    const sectionTitle = document.querySelector('.section h2');
    sectionTitle.textContent = 'Add New Question';
    sectionTitle.style.color = ''; // Reset to default color
    
    const submitButton = newForm.querySelector('button[type="submit"]');
    submitButton.textContent = 'Add Question';
    submitButton.style.backgroundColor = ''; // Reset to default color
    
    // Remove edit mode indicator if it exists
    const indicator = newForm.querySelector('.edit-mode-indicator');
    if (indicator) {
        indicator.remove();
    }
    
    // Remove cancel button if exists
    const cancelButton = newForm.querySelector('.cancel-btn');
    if (cancelButton) {
        cancelButton.remove();
    }
    
    // Add single add listener
    newForm.addEventListener('submit', handleAddQuestion);
}

function toggleInfo(questionId) {
    const infoElement = document.getElementById(`info-${questionId}`);
    const isHidden = infoElement.style.display === 'none';
    infoElement.style.display = isHidden ? 'block' : 'none';
}

async function deleteQuestion(questionId) {
    if (!confirm('Are you sure you want to delete this question?')) {
        return;
    }

    const token = localStorage.getItem('token');
    if (!token) {
        alert('You must be logged in to delete questions');
        return;
    }

    try {
        const response = await fetch(`/api/questions/${questionId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Failed to delete question');
        }

        await loadQuestions(); // Refresh the questions list
        alert('Question deleted successfully!');
    } catch (error) {
        console.error('Error:', error);
        alert('Error deleting question: ' + error.message);
    }
}

function toggleSubQuestionSection() {
    const hasSubQuestions = document.getElementById('hasSubQuestions').checked;
    const subQuestionsSection = document.getElementById('subQuestionsSection');
    subQuestionsSection.style.display = hasSubQuestions ? 'block' : 'none';
    
    if (hasSubQuestions && document.querySelectorAll('.sub-question').length === 0) {
        addSubQuestionField(); // Add first sub-question automatically
    }
}

function addSubQuestionField() {
    const subQuestionsList = document.getElementById('subQuestionsList');
    const subQuestionCount = document.querySelectorAll('.sub-question').length;
    const subIdentifier = String.fromCharCode(97 + subQuestionCount); // a, b, c, etc.

    const subQuestionDiv = document.createElement('div');
    subQuestionDiv.className = 'sub-question';
    subQuestionDiv.innerHTML = `
        <h4>Sub-Question ${subIdentifier})</h4>
        <textarea placeholder="Enter sub-question text" class="sub-question-text"></textarea>
        <textarea placeholder="Enter mark scheme" class="sub-question-mark-scheme"></textarea>
        <input type="number" placeholder="Points" class="sub-question-points" min="1">
        <button type="button" onclick="removeSubQuestion(this)">Remove</button>
    `;

    subQuestionsList.appendChild(subQuestionDiv);
}

function removeSubQuestion(button) {
    button.closest('.sub-question').remove();
    // Renumber remaining sub-questions
    document.querySelectorAll('.sub-question h4').forEach((header, index) => {
        header.textContent = `Sub-Question ${String.fromCharCode(97 + index)})`;
    });
}

function handleQuestionTypeChange() {
    const questionType = document.getElementById('questionType').value;
    const standardFields = document.getElementById('standardFields');
    const subQuestionsSection = document.getElementById('subQuestionsSection');
    const multipleChoiceSection = document.getElementById('multipleChoiceSection');
    const pointsField = document.querySelector('.form-group:has(#points)');
    const markSchemeInput = document.getElementById('markScheme');

    // Hide all sections first
    standardFields.style.display = 'none';
    subQuestionsSection.style.display = 'none';
    multipleChoiceSection.style.display = 'none';

    // Disable required attribute on hidden fields
    if (markSchemeInput) {
        markSchemeInput.required = false;
    }

    // Show/hide points field based on question type
    if (pointsField) {
        pointsField.style.display = questionType === 'WITH_SUBSECTIONS' ? 'none' : 'block';
        const pointsInput = document.getElementById('points');
        if (pointsInput) {
            pointsInput.required = questionType !== 'WITH_SUBSECTIONS';
        }
    }

    // Show relevant section based on question type
    switch (questionType) {
        case 'MULTIPLE_CHOICE':
            multipleChoiceSection.style.display = 'block';
            if (document.querySelectorAll('.option').length === 0) {
                addOptionField();
                addOptionField();
                addOptionField();
                addOptionField();
            }
            break;
        case 'STANDARD':
            standardFields.style.display = 'block';
            if (markSchemeInput) {
                markSchemeInput.required = true;
            }
            break;
        case 'WITH_SUBSECTIONS':
            subQuestionsSection.style.display = 'block';
            if (document.querySelectorAll('.sub-question').length === 0) {
                addSubQuestionField();
            }
            break;
    }
}

function addOptionField() {
    const optionsList = document.getElementById('optionsList');
    const optionCount = document.querySelectorAll('.option').length;
    const letterOption = String.fromCharCode(65 + optionCount); // A, B, C, D...

    const optionDiv = document.createElement('div');
    optionDiv.className = 'option';
    optionDiv.innerHTML = `
        <div class="option-row">
            <span class="option-letter">${letterOption}.</span>
            <input type="text" placeholder="Enter option ${letterOption}" class="option-text" required>
            <label class="correct-label">
                <input type="radio" name="correctOption" value="${optionCount}" required>
                Correct Answer
            </label>
            ${optionCount >= 4 ? '<button type="button" onclick="removeOption(this)" class="remove-btn">Remove</button>' : ''}
        </div>
    `;

    optionsList.appendChild(optionDiv);
}

function removeOption(button) {
    button.closest('.option').remove();
    // Renumber remaining options
    document.querySelectorAll('.option').forEach((option, index) => {
        option.querySelector('.option-text').placeholder = `Option ${index + 1}`;
    });
}

// Add this function to initialize the form when the page loads
function initializeForm() {
    // Show question fields by default
    document.getElementById('questionFields').style.display = 'block';
    // Set question type to STANDARD and show relevant fields
    document.getElementById('questionType').value = 'STANDARD';
    handleQuestionTypeChange();
}

// Initialize when page loads
document.addEventListener('DOMContentLoaded', () => {
    if (!currentBankId) {
        window.location.href = '/dashboard.html';
        return;
    }
    loadBankDetails();
    loadQuestions();
    initializeForm();
    loadTopics();
    loadTopicsSection();
    renderMathJax();
});

async function loadTopics() {
    try {
        const response = await fetch(`/api/topics/bank/${currentBankId}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        const topics = await response.json();
        const topicSelect = document.getElementById('topicSelect');
        topicSelect.innerHTML = '<option value="">Select Topic</option>';
        topics.forEach(topic => {
            topicSelect.innerHTML += `<option value="${topic.id}">${topic.name}</option>`;
        });
    } catch (error) {
        console.error('Error loading topics:', error);
    }
}

async function showNewTopicInput() {
    const modal = document.getElementById('newTopicModal');
    modal.style.display = 'flex';
    document.getElementById('modalTopicInput').focus();
    await loadModalTopics();
}

async function loadModalTopics() {
    try {
        // Get all topics with their question counts
        const response = await fetch('/api/topics', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        const allTopics = await response.json();
        
        // Get topics used in this bank
        const bankTopicsResponse = await fetch(`/api/questions/bank/${currentBankId}/topics`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        const bankTopics = await bankTopicsResponse.json();
        
        // Split topics into bank-specific and others
        const bankTopicIds = new Set(bankTopics.map(t => t.id));
        const bankSpecificTopics = allTopics.filter(t => bankTopicIds.has(t.id));
        const otherTopics = allTopics.filter(t => !bankTopicIds.has(t.id));
        
        displayBankTopics(bankSpecificTopics);
        displayOtherTopics(otherTopics);
    } catch (error) {
        console.error('Error loading topics:', error);
    }
}

function displayBankTopics(topics) {
    const topicsList = document.getElementById('bankTopicsList');
    topicsList.innerHTML = topics.map(topic => `
        <div class="col-md-4 mb-3">
            <div class="topic-card">
                <div class="topic-content">
                    <h4 class="topic-name">${topic.name}</h4>
                    <span class="question-count">${topic.questionCount} question${topic.questionCount !== 1 ? 's' : ''}</span>
                </div>
                <button onclick="deleteTopic(${topic.id}, true)" 
                        class="btn btn-danger delete-btn">
                    Delete
                </button>
            </div>
        </div>
    `).join('');
}

function displayOtherTopics(topics) {
    const topicsList = document.getElementById('otherTopicsList');
    topicsList.innerHTML = topics.map(topic => `
        <div class="col-md-4 mb-3">
            <div class="topic-card">
                <div class="topic-content">
                    <h4 class="topic-name">${topic.name}</h4>
                    <span class="question-count">${topic.questionCount} question${topic.questionCount !== 1 ? 's' : ''}</span>
                </div>
                <button onclick="deleteTopic(${topic.id})" 
                        class="btn ${topic.questionCount > 0 ? 'btn-secondary' : 'btn-danger'} delete-btn"
                        ${topic.questionCount > 0 ? 'disabled title="Cannot delete topics with questions"' : ''}>
                    Delete
                </button>
            </div>
        </div>
    `).join('');
}

async function deleteTopic(id, isInCurrentBank) {
    if (!confirm('Are you sure you want to delete this topic?')) return;

    try {
        const url = isInCurrentBank 
            ? `/api/topics/${id}?bankId=${currentBankId}`
            : `/api/topics/${id}`;
            
        const response = await fetch(url, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            }
        });
        
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || 'Failed to delete topic');
        }
        
        await loadModalTopics();
        await loadTopics();
    } catch (error) {
        console.error('Error:', error);
        alert(error.message);
    }
}

async function createNewTopicFromModal() {
    const newTopicName = document.getElementById('modalTopicInput').value.trim();
    if (!newTopicName) return;

    try {
        const response = await fetch('/api/topics', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ 
                name: newTopicName,
                bank: { id: currentBankId }
            })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to create topic');
        }

        const newTopic = await response.json();
        document.getElementById('modalTopicInput').value = '';
        
        // Refresh both the modal list and dropdown
        await Promise.all([
            loadModalTopics(),
            loadTopics()
        ]);

        closeTopicModal();
    } catch (error) {
        console.error('Error creating topic:', error);
        alert(error.message);
    }
}

async function loadTopicsSection() {
    try {
        const response = await fetch(`/api/questions/bank/${currentBankId}/topics`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        const topics = await response.json();
        displayTopics(topics);
    } catch (error) {
        console.error('Error loading topics:', error);
    }
}

function displayTopics(topics) {
    const topicsList = document.getElementById('topicsList');
    if (!topics || topics.length === 0) {
        topicsList.innerHTML = `
            <div class="no-questions-message">
                <p>No topics have been created in this question bank yet.</p>
                <p>Add a topic when creating a new question, or use the "Add New Topic" button!</p>
            </div>
        `;
        return;
    }

    topicsList.innerHTML = topics.map(topic => `
        <div class="topic-card" onclick="navigateToTopic(${topic.id})">
            <h3>${topic.name}</h3>
            <span class="question-count">${topic.questionCount} questions</span>
        </div>
    `).join('');
}

// Add this new function to handle navigation
async function navigateToTopic(topicId) {
    try {
        // First verify we can access the topic
        const response = await fetch(`/api/questions/bank/${currentBankId}/topic/${topicId}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (response.ok) {
            // If successful, navigate to the topic page
            window.location.href = `/topic.html?topicId=${topicId}&bankId=${currentBankId}`;
        } else {
            console.error('Failed to fetch topic questions');
        }
    } catch (error) {
        console.error('Error:', error);
    }
}

// Add this function to your existing questionbank.js
function createQuiz() {
    if (!currentBankId) {
        alert('Question bank ID not found');
        return;
    }
    window.location.href = `/quiz.html?bankId=${currentBankId}`;
}

// Helper function to get bank ID from URL if you don't already have it
function getBankId() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('bankId');
}

function closeTopicModal() {
    const modal = document.getElementById('newTopicModal');
    modal.style.display = 'none';
    document.getElementById('modalTopicInput').value = ''; // Clear the input field
}

// Add event listener for clicking outside the modal to close it
document.addEventListener('DOMContentLoaded', () => {
    const modal = document.getElementById('newTopicModal');
    
    // Close when clicking outside the modal content
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            closeTopicModal();
        }
    });

    // Close when pressing Escape key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && modal.style.display === 'flex') {
            closeTopicModal();
        }
    });
});

async function togglePublish(bankId) {
    try {
        const response = await fetch(`/api/questionbanks/${bankId}/publish`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Failed to toggle publish status');
        }

        // Refresh the bank details to update UI
        loadBankDetails();
    } catch (error) {
        console.error('Error:', error);
        alert('Failed to update publish status: ' + error.message);
    }
}

// Add these new functions

function insertEquation(targetId) {
    const textarea = document.getElementById(targetId);
    const template = '\\( \\)';
    const cursorPosition = textarea.selectionStart;
    
    // Insert template at cursor position
    const textBefore = textarea.value.substring(0, cursorPosition);
    const textAfter = textarea.value.substring(cursorPosition);
    textarea.value = textBefore + template + textAfter;
    
    // Place cursor between the parentheses
    const newCursorPosition = cursorPosition + 3;
    textarea.setSelectionRange(newCursorPosition, newCursorPosition);
    textarea.focus();
}

// Add this to your existing functions that display questions
function renderMathInElement(element) {
    if (window.MathJax) {
        MathJax.typesetPromise([element]).catch((err) => console.error('MathJax error:', err));
    }
}

// Add this function to handle MathJax rendering
function renderMathJax() {
    if (window.MathJax) {
        MathJax.typesetPromise && MathJax.typesetPromise();
    }
}

