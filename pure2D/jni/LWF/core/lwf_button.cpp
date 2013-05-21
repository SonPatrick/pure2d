#include "lwf_button.h"
#include "lwf_core.h"
#include "lwf_data.h"
#include "lwf_utility.h"

namespace LWF {

typedef ButtonEventHandlers EventType;
typedef Format::ButtonCondition Condition;

Button::Button(LWF *l, Movie *p, int objId, int instId, int mId, int cId)
	: IObject(l, p, Format::Object::BUTTON, objId, instId)
{
	matrixId = mId;
	colorTransformId = cId;

	hitX = FLT_MIN;
	hitY = FLT_MIN;

	if (objId >= 0) {
		data = &lwf->data->buttons[objId];
		dataMatrixId = data->matrixId;
		width = data->width;
		height = data->height;
	} else {
		data = 0;
		width = 0;
		height = 0;
	}
	buttonLink = 0;

	m_handler.Add(lwf->GetButtonEventHandlers(this));
	if (!m_handler.Empty())
		m_handler.Call(EventType::LOAD, this);
}

void Button::AddHandlers(const ButtonEventHandlers *h)
{
	m_handler.Add(h);
}

void Button::Exec(int mId, int cId)
{
	IObject::Exec(mId, cId);

	EnterFrame();
}

void Button::Update(const Matrix *m, const ColorTransform *c)
{
	IObject::Update(m, c);

	if (!m_handler.Empty())
		m_handler.Call(EventType::UPDATE, this);
}

void Button::Render(bool v, int rOffset)
{
	if (v && !m_handler.Empty())
		m_handler.Call(EventType::RENDER, this);
}

void Button::Destroy()
{
	lwf->ClearFocus(this);
	lwf->ClearPressed(this);

	if (!m_handler.Empty())
		m_handler.Call(EventType::UNLOAD, this);

	IObject::Destroy();
}

void Button::LinkButton()
{
	buttonLink = lwf->buttonHead;
	lwf->buttonHead = this;
}

bool Button::CheckHit(float px, float py)
{
	float x, y;
	Utility::InvertMatrix(&m_invert, &matrix);
	Utility::CalcMatrixToPoint(x, y, px, py, &m_invert);
	if (x >= 0.0f && x < (float)data->width &&
			y >= 0.0f && y < (float)data->height) {
		hitX = x;
		hitY = y;
		return true;
	}
	hitX = FLT_MIN;
	hitY = FLT_MIN;
	return false;
}

void Button::EnterFrame()
{
	if (!m_handler.Empty())
		m_handler.Call(EventType::ENTERFRAME, this);
}

void Button::RollOver()
{
	if (!m_handler.Empty())
		m_handler.Call(EventType::ROLLOVER, this);

	PlayAnimation(Condition::ROLLOVER);
}

void Button::RollOut()
{
	if (!m_handler.Empty())
		m_handler.Call(EventType::ROLLOUT, this);

	PlayAnimation(Condition::ROLLOUT);
}

void Button::Press()
{
	if (!m_handler.Empty())
		m_handler.Call(EventType::PRESS, this);

	PlayAnimation(Condition::PRESS);
}

void Button::Release()
{
	if (!m_handler.Empty())
		m_handler.Call(EventType::RELEASE, this);

	PlayAnimation(Condition::RELEASE);
}

void Button::KeyPress(int code)
{
	if (!m_handler.Empty())
		m_handler.CallKEYPRESS(this, code);

	PlayAnimation(Condition::KEYPRESS, code);
}

void Button::PlayAnimation(int condition, int code)
{
	if (!data)
		return;

	for (int i = 0; i < data->conditions; ++i) {
		const Format::ButtonCondition &c =
			lwf->data->buttonConditions[data->conditionId + i];
		if ((c.condition & (int)condition) != 0 &&
				(condition != Condition::KEYPRESS || c.keyCode == code)) {
			lwf->PlayAnimation(c.animationId, parent, this);
		}
	}
}

}	// namespace LWF
