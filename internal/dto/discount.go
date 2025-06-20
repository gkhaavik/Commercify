package dto

import (
	"time"

	"github.com/zenfulcode/commercify/internal/application/usecase"
	"github.com/zenfulcode/commercify/internal/domain/entity"
	"github.com/zenfulcode/commercify/internal/domain/money"
)

// DiscountDTO represents a discount in the system
type DiscountDTO struct {
	ID               uint      `json:"id"`
	Code             string    `json:"code"`
	Type             string    `json:"type"`
	Method           string    `json:"method"`
	Value            float64   `json:"value"`
	MinOrderValue    float64   `json:"min_order_value"`
	MaxDiscountValue float64   `json:"max_discount_value"`
	ProductIDs       []uint    `json:"product_ids,omitempty"`
	CategoryIDs      []uint    `json:"category_ids,omitempty"`
	StartDate        time.Time `json:"start_date"`
	EndDate          time.Time `json:"end_date"`
	UsageLimit       int       `json:"usage_limit"`
	CurrentUsage     int       `json:"current_usage"`
	Active           bool      `json:"active"`
	CreatedAt        time.Time `json:"created_at"`
	UpdatedAt        time.Time `json:"updated_at"`
}

// AppliedDiscountDTO represents an applied discount in a checkout
type AppliedDiscountDTO struct {
	ID     uint    `json:"id"`
	Code   string  `json:"code"`
	Type   string  `json:"type"`
	Method string  `json:"method"`
	Value  float64 `json:"value"`
	Amount float64 `json:"amount"`
}

// CreateDiscountRequest represents the data needed to create a new discount
type CreateDiscountRequest struct {
	Code             string    `json:"code"`
	Type             string    `json:"type"`
	Method           string    `json:"method"`
	Value            float64   `json:"value"`
	MinOrderValue    float64   `json:"min_order_value,omitempty"`
	MaxDiscountValue float64   `json:"max_discount_value,omitempty"`
	ProductIDs       []uint    `json:"product_ids,omitempty"`
	CategoryIDs      []uint    `json:"category_ids,omitempty"`
	StartDate        time.Time `json:"start_date,omitempty"`
	EndDate          time.Time `json:"end_date,omitempty"`
	UsageLimit       int       `json:"usage_limit,omitempty"`
}

// UpdateDiscountRequest represents the data needed to update a discount
type UpdateDiscountRequest struct {
	Code             string    `json:"code,omitempty"`
	Type             string    `json:"type,omitempty"`
	Method           string    `json:"method,omitempty"`
	Value            float64   `json:"value,omitempty"`
	MinOrderValue    float64   `json:"min_order_value,omitempty"`
	MaxDiscountValue float64   `json:"max_discount_value,omitempty"`
	ProductIDs       []uint    `json:"product_ids,omitempty"`
	CategoryIDs      []uint    `json:"category_ids,omitempty"`
	StartDate        time.Time `json:"start_date"`
	EndDate          time.Time `json:"end_date"`
	UsageLimit       int       `json:"usage_limit,omitempty"`
	Active           bool      `json:"active"`
}

// ValidateDiscountRequest represents the data needed to validate a discount code
type ValidateDiscountRequest struct {
	DiscountCode string `json:"discount_code"`
}

// ValidateDiscountResponse represents the response for discount validation
type ValidateDiscountResponse struct {
	Valid            bool    `json:"valid"`
	Reason           string  `json:"reason,omitempty"`
	DiscountID       uint    `json:"discount_id,omitempty"`
	Code             string  `json:"code,omitempty"`
	Type             string  `json:"type,omitempty"`
	Method           string  `json:"method,omitempty"`
	Value            float64 `json:"value,omitempty"`
	MinOrderValue    float64 `json:"min_order_value,omitempty"`
	MaxDiscountValue float64 `json:"max_discount_value,omitempty"`
}

func (r CreateDiscountRequest) ToUseCaseInput() usecase.CreateDiscountInput {
	if r.MinOrderValue < 0 {
		r.MinOrderValue = 0
	}
	if r.MaxDiscountValue < 0 {
		r.MaxDiscountValue = 0
	}
	if r.UsageLimit < 0 {
		r.UsageLimit = 0
	}
	if r.StartDate.IsZero() {
		r.StartDate = time.Now().Local()
	}
	if r.EndDate.IsZero() {
		r.EndDate = time.Now().Local().AddDate(1, 0, 0) // Default to 1 year from now
	}
	if r.ProductIDs == nil {
		r.ProductIDs = []uint{}
	}
	if r.CategoryIDs == nil {
		r.CategoryIDs = []uint{}
	}

	return usecase.CreateDiscountInput{
		Code:             r.Code,
		Type:             r.Type,
		Method:           r.Method,
		Value:            r.Value,
		MinOrderValue:    r.MinOrderValue,
		MaxDiscountValue: r.MaxDiscountValue,
		ProductIDs:       r.ProductIDs,
		CategoryIDs:      r.CategoryIDs,
		StartDate:        r.StartDate,
		EndDate:          r.EndDate,
		UsageLimit:       r.UsageLimit,
	}
}

func (r UpdateDiscountRequest) ToUseCaseInput() usecase.UpdateDiscountInput {
	return usecase.UpdateDiscountInput{
		Code:             r.Code,
		Type:             r.Type,
		Method:           r.Method,
		Value:            r.Value,
		MinOrderValue:    r.MinOrderValue,
		MaxDiscountValue: r.MaxDiscountValue,
		ProductIDs:       r.ProductIDs,
		CategoryIDs:      r.CategoryIDs,
		StartDate:        r.StartDate,
		EndDate:          r.EndDate,
		UsageLimit:       r.UsageLimit,
		Active:           r.Active,
	}
}

func DiscountCreateResponse(discount *entity.Discount) ResponseDTO[DiscountDTO] {
	return SuccessResponseWithMessage(toDiscountDTO(discount), "Discount created successfully")
}

func DiscountRetrieveResponse(discount *entity.Discount) ResponseDTO[DiscountDTO] {
	return SuccessResponse(toDiscountDTO(discount))
}

func DiscountUpdateResponse(discount *entity.Discount) ResponseDTO[DiscountDTO] {
	return SuccessResponseWithMessage(toDiscountDTO(discount), "Discount updated successfully")
}

func DiscountDeleteResponse() ResponseDTO[any] {
	return SuccessResponseMessage("Discount deleted successfully")
}

func DiscountListResponse(discounts []*entity.Discount, totalCount, page, pageSize int) ListResponseDTO[DiscountDTO] {
	return ListResponseDTO[DiscountDTO]{
		Success: true,
		Data:    ConvertDiscountListToDTO(discounts),
		Pagination: PaginationDTO{
			Page:     page,
			PageSize: pageSize,
			Total:    totalCount,
		},
		Message: "Discounts retrieved successfully",
	}
}

// ConvertToDiscountDTO converts a domain discount entity to a DTO
func toDiscountDTO(discount *entity.Discount) DiscountDTO {
	if discount == nil {
		return DiscountDTO{}
	}

	return DiscountDTO{
		ID:               discount.ID,
		Code:             discount.Code,
		Type:             string(discount.Type),
		Method:           string(discount.Method),
		Value:            discount.Value,
		MinOrderValue:    money.FromCents(discount.MinOrderValue),
		MaxDiscountValue: money.FromCents(discount.MaxDiscountValue),
		ProductIDs:       discount.ProductIDs,
		CategoryIDs:      discount.CategoryIDs,
		StartDate:        discount.StartDate,
		EndDate:          discount.EndDate,
		UsageLimit:       discount.UsageLimit,
		CurrentUsage:     discount.CurrentUsage,
		Active:           discount.Active,
		CreatedAt:        discount.CreatedAt,
		UpdatedAt:        discount.UpdatedAt,
	}
}

// ConvertToAppliedDiscountDTO converts a domain applied discount entity to a DTO
func ConvertToAppliedDiscountDTO(appliedDiscount *entity.AppliedDiscount) AppliedDiscountDTO {
	if appliedDiscount == nil {
		return AppliedDiscountDTO{}
	}

	return AppliedDiscountDTO{
		ID:     appliedDiscount.DiscountID,
		Code:   appliedDiscount.DiscountCode,
		Type:   "", // We don't have this info in the AppliedDiscount
		Method: "", // We don't have this info in the AppliedDiscount
		Value:  0,  // We don't have this info in the AppliedDiscount
		Amount: money.FromCents(appliedDiscount.DiscountAmount),
	}
}

// ConvertDiscountListToDTO converts a slice of domain discount entities to DTOs
func ConvertDiscountListToDTO(discounts []*entity.Discount) []DiscountDTO {
	dtos := make([]DiscountDTO, len(discounts))
	for i, discount := range discounts {
		dtos[i] = toDiscountDTO(discount)
	}
	return dtos
}
